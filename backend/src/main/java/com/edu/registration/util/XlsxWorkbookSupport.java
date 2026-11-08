package com.edu.registration.util;

import com.edu.registration.config.GradeImportProperties;
import com.edu.registration.exception.ApiException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Tiện ích đọc và tạo tệp Excel OOXML (.xlsx) an toàn cho chức năng import điểm (QLSV-73).
 * <ul>
 *     <li>Kiểm tra magic bytes thực tế (từ chối CSV, văn bản thuần, .xls OLE2 hoặc tệp giả mạo phần mở rộng).</li>
 *     <li>Chống Zip Bomb (giới hạn số entry và tổng dung lượng giải nén).</li>
 *     <li>Chống XXE (vô hiệu hóa hoàn toàn DTD và external entities trên StAX parser).</li>
 *     <li>Không bao giờ thực thi công thức Excel; phát hiện và đánh dấu các ô chứa thẻ {@code <f>} hoặc biểu thức công thức.</li>
 *     <li>Giới hạn số lượng sheet, số dòng dữ liệu và thời gian xử lý tối đa theo cấu hình.</li>
 * </ul>
 */
public final class XlsxWorkbookSupport {

    public static final String DEFAULT_SHEET_NAME = "BangDiem";
    public static final String XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final byte[] ZIP_MAGIC = new byte[]{0x50, 0x4B, 0x03, 0x04};
    private static final byte[] OLE2_XLS_MAGIC = new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0};

    private XlsxWorkbookSupport() {
    }

    public enum CellValueKind {
        BLANK,
        STRING,
        NUMERIC,
        BOOLEAN,
        FORMULA,
        ERROR,
        DATE
    }

    public record ParsedCell(
            int rowNumber,
            int colIndex,
            String colLetter,
            CellValueKind kind,
            String rawText,
            String formulaText
    ) {
        public boolean isBlank() {
            return kind == CellValueKind.BLANK || (rawText == null || rawText.isBlank()) && kind != CellValueKind.FORMULA && kind != CellValueKind.ERROR;
        }

        public boolean isFormula() {
            if (kind == CellValueKind.FORMULA) {
                return true;
            }
            if (rawText != null) {
                String trimmed = rawText.trim();
                return trimmed.startsWith("=");
            }
            return false;
        }
    }

    public record ParsedRow(
            int rowNumber,
            Map<Integer, ParsedCell> cellsByCol
    ) {
        public ParsedCell cellAt(int colIndex) {
            return cellsByCol.get(colIndex);
        }

        public boolean isCompletelyBlank() {
            if (cellsByCol == null || cellsByCol.isEmpty()) {
                return true;
            }
            for (ParsedCell cell : cellsByCol.values()) {
                if (cell != null && !cell.isBlank()) {
                    return false;
                }
            }
            return true;
        }
    }

    public record ParsedSheet(
            String sheetName,
            ParsedRow headerRow,
            List<ParsedRow> dataRows
    ) {
    }

    public record TemplateStudentRow(
            String maSinhVien,
            String hoTen,
            Double diemQuaTrinh,
            Double diemCuoiKy
    ) {
    }

    /**
     * Tạo workbook .xlsx mẫu chuẩn để giảng viên tải về nhập điểm.
     */
    public static byte[] createGradeTemplateWorkbook(List<TemplateStudentRow> students) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("maSinhVien", "hoTen", "diemQuaTrinh", "diemCuoiKy"));
        if (students != null) {
            for (TemplateStudentRow sv : students) {
                List<Object> row = new ArrayList<>(4);
                row.add(sv.maSinhVien() != null ? sv.maSinhVien() : "");
                row.add(sv.hoTen() != null ? sv.hoTen() : "");
                row.add(sv.diemQuaTrinh());
                row.add(sv.diemCuoiKy());
                rows.add(row);
            }
        }
        return createWorkbookBytes(Map.of(DEFAULT_SHEET_NAME, rows));
    }

    /**
     * Tạo workbook .xlsx từ danh sách các sheet (phục vụ tạo template và kiểm thử).
     */
    public static byte[] createWorkbookBytes(Map<String, List<List<Object>>> sheetsData) {
        Map<String, List<List<Object>>> orderedSheets = (sheetsData == null || sheetsData.isEmpty())
                ? Map.of(DEFAULT_SHEET_NAME, List.of())
                : new LinkedHashMap<>(sheetsData);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {

            writeZipEntry(zip, "[Content_Types].xml", buildContentTypesXml(orderedSheets.size()));
            writeZipEntry(zip, "_rels/.rels", buildRootRelsXml());
            writeZipEntry(zip, "xl/workbook.xml", buildWorkbookXml(new ArrayList<>(orderedSheets.keySet())));
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml(orderedSheets.size()));
            writeZipEntry(zip, "xl/styles.xml", buildStylesXml());

            int sheetIdx = 1;
            for (Map.Entry<String, List<List<Object>>> entry : orderedSheets.entrySet()) {
                writeZipEntry(zip, "xl/worksheets/sheet" + sheetIdx + ".xml", buildSheetXml(entry.getValue()));
                sheetIdx++;
            }

            zip.finish();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể khởi tạo tệp mẫu Excel .xlsx", ex);
        }
    }

    /**
     * Phân tích tệp .xlsx từ đường dẫn tạm thời với các rào chắn bảo mật và giới hạn cấu hình.
     */
    public static ParsedSheet parseWorkbook(Path filePath, GradeImportProperties properties) {
        GradeImportProperties props = properties != null ? properties : GradeImportProperties.defaults();
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(props.parseTimeoutMs());

        verifyFileHeaderMagic(filePath, props.maxFileSizeBytes());
        checkDeadline(deadlineNanos, props.parseTimeoutMs());

        Map<String, byte[]> zipEntries = readZipEntriesSafely(filePath, props, deadlineNanos);
        checkDeadline(deadlineNanos, props.parseTimeoutMs());

        byte[] contentTypesBytes = zipEntries.get("[Content_Types].xml");
        byte[] workbookBytes = zipEntries.get("xl/workbook.xml");
        if (contentTypesBytes == null || workbookBytes == null) {
            throw ApiException.badRequest("Tệp không phải là workbook Excel (.xlsx) hợp lệ");
        }

        validateContentTypes(contentTypesBytes, deadlineNanos, props.parseTimeoutMs());

        List<SheetRef> sheets = parseWorkbookSheets(workbookBytes, deadlineNanos, props.parseTimeoutMs());
        if (sheets.isEmpty()) {
            throw ApiException.badRequest("Workbook Excel không có sheet dữ liệu nào");
        }
        if (sheets.size() > props.maxSheets()) {
            throw ApiException.badRequest("Số lượng sheet trong workbook (" + sheets.size()
                    + ") vượt quá giới hạn tối đa cho phép (" + props.maxSheets() + " sheet)");
        }

        Map<String, String> relTargets = parseWorkbookRels(
                zipEntries.get("xl/_rels/workbook.xml.rels"), deadlineNanos, props.parseTimeoutMs());

        List<String> sharedStrings = parseSharedStrings(
                zipEntries.get("xl/sharedStrings.xml"), deadlineNanos, props.parseTimeoutMs());

        Set<Integer> dateStyleIndices = parseDateStyleIndices(
                zipEntries.get("xl/styles.xml"), deadlineNanos, props.parseTimeoutMs());

        SheetRef primarySheet = sheets.get(0);
        String sheetPath = resolveSheetPath(primarySheet, relTargets, zipEntries);
        byte[] sheetBytes = zipEntries.get(sheetPath);
        if (sheetBytes == null) {
            throw ApiException.badRequest("Không tìm thấy dữ liệu của sheet '" + primarySheet.name() + "' trong tệp .xlsx");
        }

        return parseWorksheet(
                primarySheet.name(),
                sheetBytes,
                sharedStrings,
                dateStyleIndices,
                props,
                deadlineNanos
        );
    }

    private static void verifyFileHeaderMagic(Path filePath, long maxFileSizeBytes) {
        try {
            long size = Files.size(filePath);
            if (size <= 0) {
                throw ApiException.badRequest("Tệp tải lên đang trống");
            }
            if (size > maxFileSizeBytes) {
                throw ApiException.badRequest("Kích thước tệp vượt quá giới hạn tối đa cho phép (" + maxFileSizeBytes + " bytes)");
            }
            byte[] header = new byte[8];
            int read;
            try (InputStream in = Files.newInputStream(filePath)) {
                read = in.read(header);
            }
            if (read < 4) {
                throw ApiException.badRequest("Tệp tải lên không phải là định dạng .xlsx hợp lệ");
            }
            if (startsWith(header, OLE2_XLS_MAGIC)) {
                throw ApiException.badRequest("Không hỗ trợ định dạng Excel cũ (.xls). Vui lòng tải lên tệp .xlsx");
            }
            if (!startsWith(header, ZIP_MAGIC)) {
                throw ApiException.badRequest("Nội dung tệp không phải là workbook .xlsx hợp lệ (từ chối CSV hoặc tệp đổi tên)");
            }
        } catch (IOException ex) {
            throw ApiException.badRequest("Không thể đọc tệp tải lên");
        }
    }

    private static boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (source[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, byte[]> readZipEntriesSafely(
            Path filePath,
            GradeImportProperties props,
            long deadlineNanos
    ) {
        Map<String, byte[]> entries = new HashMap<>();
        long totalUncompressed = 0L;
        int entryCount = 0;
        byte[] buffer = new byte[8192];

        try (InputStream fis = Files.newInputStream(filePath);
             ZipInputStream zis = new ZipInputStream(fis, StandardCharsets.UTF_8)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                checkDeadline(deadlineNanos, props.parseTimeoutMs());
                entryCount++;
                if (entryCount > props.maxZipEntries()) {
                    throw ApiException.badRequest("Tệp .xlsx chứa quá nhiều thành phần nén bất thường (vượt giới hạn an toàn)");
                }
                String name = normalizeZipEntryName(entry.getName());
                if (name.contains("..") || name.startsWith("/")) {
                    throw ApiException.badRequest("Phát hiện đường dẫn không an toàn bên trong tệp .xlsx");
                }
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.endsWith("vbaproject.bin") || lower.contains("macrosheets")) {
                    throw ApiException.badRequest("Tệp Excel chứa macro/VBA không được phép tải lên");
                }
                if (entry.isDirectory()) {
                    continue;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                while ((len = zis.read(buffer)) != -1) {
                    checkDeadline(deadlineNanos, props.parseTimeoutMs());
                    totalUncompressed += len;
                    if (totalUncompressed > props.maxZipEntrySizeBytes()) {
                        throw ApiException.badRequest("Dung lượng giải nén của tệp .xlsx vượt quá giới hạn an toàn");
                    }
                    baos.write(buffer, 0, len);
                }
                entries.put(name, baos.toByteArray());
            }
        } catch (ZipException ex) {
            throw ApiException.badRequest("Tệp .xlsx bị hỏng cấu trúc nén hoặc không hợp lệ");
        } catch (IOException ex) {
            throw ApiException.badRequest("Không thể giải nén tệp .xlsx");
        }

        if (entries.isEmpty()) {
            throw ApiException.badRequest("Tệp .xlsx không hợp lệ hoặc bị rỗng");
        }
        return entries;
    }

    private static String normalizeZipEntryName(String name) {
        if (name == null) {
            return "";
        }
        String normalized = name.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static XMLInputFactory createSafeXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        return factory;
    }

    private static void validateContentTypes(byte[] xmlBytes, long deadlineNanos, long timeoutMs) {
        XMLInputFactory factory = createSafeXmlInputFactory();
        boolean hasSpreadsheetMain = false;
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xmlBytes));
            while (reader.hasNext()) {
                checkDeadline(deadlineNanos, timeoutMs);
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    if ("Override".equals(localName) || "Default".equals(localName)) {
                        String contentType = getAttr(reader, "ContentType");
                        if (contentType != null) {
                            String lower = contentType.toLowerCase(Locale.ROOT);
                            if (lower.contains("macroenabled") || lower.contains("vba")) {
                                throw ApiException.badRequest("Không chấp nhận workbook Excel có chứa macro (.xlsm)");
                            }
                            if (lower.contains("spreadsheetml.sheet.main+xml")) {
                                hasSpreadsheetMain = true;
                            }
                        }
                    }
                }
            }
            reader.close();
        } catch (XMLStreamException ex) {
            throw ApiException.badRequest("Cấu trúc XML trong tệp .xlsx bị lỗi");
        }
        if (!hasSpreadsheetMain) {
            throw ApiException.badRequest("Tệp không phải là định dạng bảng tính Excel (.xlsx) tiêu chuẩn");
        }
    }

    private record SheetRef(String name, String sheetId, String rId) {
    }

    private static List<SheetRef> parseWorkbookSheets(byte[] xmlBytes, long deadlineNanos, long timeoutMs) {
        XMLInputFactory factory = createSafeXmlInputFactory();
        List<SheetRef> sheets = new ArrayList<>();
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xmlBytes));
            while (reader.hasNext()) {
                checkDeadline(deadlineNanos, timeoutMs);
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT && "sheet".equals(reader.getLocalName())) {
                    String name = getAttr(reader, "name");
                    String sheetId = getAttr(reader, "sheetId");
                    String rId = getAttrByLocalName(reader, "id");
                    if (name != null && !name.isBlank()) {
                        sheets.add(new SheetRef(name.trim(), sheetId, rId));
                    }
                }
            }
            reader.close();
        } catch (XMLStreamException ex) {
            throw ApiException.badRequest("Không thể đọc cấu trúc sheet trong tệp .xlsx");
        }
        return sheets;
    }

    private static Map<String, String> parseWorkbookRels(byte[] xmlBytes, long deadlineNanos, long timeoutMs) {
        if (xmlBytes == null) {
            return Map.of();
        }
        XMLInputFactory factory = createSafeXmlInputFactory();
        Map<String, String> rels = new HashMap<>();
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xmlBytes));
            while (reader.hasNext()) {
                checkDeadline(deadlineNanos, timeoutMs);
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT && "Relationship".equals(reader.getLocalName())) {
                    String id = getAttr(reader, "Id");
                    String target = getAttr(reader, "Target");
                    if (id != null && target != null) {
                        String norm = target.replace('\\', '/').trim();
                        if (norm.startsWith("/")) {
                            norm = norm.substring(1);
                        } else if (!norm.startsWith("xl/")) {
                            norm = "xl/" + norm;
                        }
                        rels.put(id, norm);
                    }
                }
            }
            reader.close();
        } catch (XMLStreamException ex) {
            throw ApiException.badRequest("Cấu trúc liên kết sheet trong tệp .xlsx bị lỗi");
        }
        return rels;
    }

    private static String resolveSheetPath(
            SheetRef sheet,
            Map<String, String> relTargets,
            Map<String, byte[]> zipEntries
    ) {
        if (sheet.rId() != null && relTargets.containsKey(sheet.rId())) {
            String candidate = relTargets.get(sheet.rId());
            if (zipEntries.containsKey(candidate)) {
                return candidate;
            }
        }
        if (zipEntries.containsKey("xl/worksheets/sheet1.xml")) {
            return "xl/worksheets/sheet1.xml";
        }
        return zipEntries.keySet().stream()
                .filter(k -> k.startsWith("xl/worksheets/") && k.endsWith(".xml"))
                .sorted()
                .findFirst()
                .orElse("xl/worksheets/sheet1.xml");
    }

    private static List<String> parseSharedStrings(byte[] xmlBytes, long deadlineNanos, long timeoutMs) {
        if (xmlBytes == null) {
            return List.of();
        }
        XMLInputFactory factory = createSafeXmlInputFactory();
        List<String> strings = new ArrayList<>();
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xmlBytes));
            StringBuilder currentSi = null;
            boolean inPhonetic = false;
            boolean inText = false;
            while (reader.hasNext()) {
                checkDeadline(deadlineNanos, timeoutMs);
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("si".equals(local)) {
                        currentSi = new StringBuilder();
                        inPhonetic = false;
                    } else if ("rPh".equals(local)) {
                        inPhonetic = true;
                    } else if ("t".equals(local) && currentSi != null && !inPhonetic) {
                        inText = true;
                    }
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    if (inText && currentSi != null) {
                        currentSi.append(reader.getText());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("t".equals(local)) {
                        inText = false;
                    } else if ("rPh".equals(local)) {
                        inPhonetic = false;
                    } else if ("si".equals(local) && currentSi != null) {
                        strings.add(currentSi.toString());
                        currentSi = null;
                    }
                }
            }
            reader.close();
        } catch (XMLStreamException ex) {
            throw ApiException.badRequest("Bảng chuỗi dùng chung (sharedStrings) trong tệp .xlsx bị lỗi");
        }
        return strings;
    }

    private static Set<Integer> parseDateStyleIndices(byte[] xmlBytes, long deadlineNanos, long timeoutMs) {
        if (xmlBytes == null) {
            return Set.of();
        }
        XMLInputFactory factory = createSafeXmlInputFactory();
        Map<Integer, String> customNumFmts = new HashMap<>();
        List<Integer> xfNumFmtIds = new ArrayList<>();
        boolean inCellXfs = false;

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xmlBytes));
            while (reader.hasNext()) {
                checkDeadline(deadlineNanos, timeoutMs);
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("numFmt".equals(local)) {
                        String idStr = getAttr(reader, "numFmtId");
                        String code = getAttr(reader, "formatCode");
                        if (idStr != null && code != null) {
                            try {
                                customNumFmts.put(Integer.parseInt(idStr.trim()), code);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    } else if ("cellXfs".equals(local)) {
                        inCellXfs = true;
                    } else if ("xf".equals(local) && inCellXfs) {
                        String idStr = getAttr(reader, "numFmtId");
                        int numFmtId = 0;
                        if (idStr != null) {
                            try {
                                numFmtId = Integer.parseInt(idStr.trim());
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        xfNumFmtIds.add(numFmtId);
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT && "cellXfs".equals(reader.getLocalName())) {
                    inCellXfs = false;
                }
            }
            reader.close();
        } catch (XMLStreamException ex) {
            throw ApiException.badRequest("Cấu trúc định dạng ô (styles) trong tệp .xlsx bị lỗi");
        }

        Set<Integer> dateStyleIndices = new HashSet<>();
        for (int i = 0; i < xfNumFmtIds.size(); i++) {
            int fmtId = xfNumFmtIds.get(i);
            if (isDateNumFmt(fmtId, customNumFmts.get(fmtId))) {
                dateStyleIndices.add(i);
            }
        }
        return dateStyleIndices;
    }

    private static boolean isDateNumFmt(int fmtId, String customCode) {
        if ((fmtId >= 14 && fmtId <= 22)
                || (fmtId >= 27 && fmtId <= 36)
                || (fmtId >= 45 && fmtId <= 47)
                || (fmtId >= 50 && fmtId <= 58)) {
            return true;
        }
        if (customCode == null || customCode.isBlank()) {
            return false;
        }
        String cleaned = customCode
                .replaceAll("\"[^\"]*\"", "")
                .replaceAll("\\[[^\\]]*\\]", "")
                .toLowerCase(Locale.ROOT);
        if (cleaned.contains("#") || cleaned.contains("0.0")) {
            return false;
        }
        return cleaned.contains("yy") || cleaned.contains("mm") || cleaned.contains("dd") || cleaned.contains("hh");
    }

    private static ParsedSheet parseWorksheet(
            String sheetName,
            byte[] sheetBytes,
            List<String> sharedStrings,
            Set<Integer> dateStyleIndices,
            GradeImportProperties props,
            long deadlineNanos
    ) {
        XMLInputFactory factory = createSafeXmlInputFactory();
        ParsedRow headerRow = null;
        List<ParsedRow> dataRows = new ArrayList<>();

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(sheetBytes));
            int currentRowNum = 0;
            Map<Integer, ParsedCell> currentRowCells = null;

            int currentColIndex = -1;
            String currentColLetter = null;
            String currentCellType = null;
            int currentStyleIndex = -1;
            boolean currentCellHasFormula = false;
            StringBuilder currentValue = null;
            StringBuilder currentFormula = null;
            StringBuilder currentInlineStr = null;
            boolean inValue = false;
            boolean inFormula = false;
            boolean inInlineText = false;
            boolean inPhonetic = false;

            while (reader.hasNext()) {
                checkDeadline(deadlineNanos, props.parseTimeoutMs());
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("row".equals(local)) {
                        String rAttr = getAttr(reader, "r");
                        if (rAttr != null && !rAttr.isBlank()) {
                            try {
                                currentRowNum = Integer.parseInt(rAttr.trim());
                            } catch (NumberFormatException ex) {
                                currentRowNum++;
                            }
                        } else {
                            currentRowNum++;
                        }
                        if (currentRowNum > props.maxRows() + 200) {
                            throw ApiException.badRequest("Số dòng trong sheet '" + sheetName
                                    + "' vượt quá giới hạn tối đa cho phép (" + props.maxRows() + " dòng dữ liệu)");
                        }
                        currentRowCells = new LinkedHashMap<>();
                        currentColIndex = -1;
                    } else if ("c".equals(local) && currentRowCells != null) {
                        String cellRef = getAttr(reader, "r");
                        if (cellRef != null && !cellRef.isBlank()) {
                            currentColLetter = extractColLetter(cellRef);
                            currentColIndex = colLetterToIndex(currentColLetter);
                        } else {
                            currentColIndex++;
                            currentColLetter = indexToColLetter(currentColIndex);
                        }
                        currentCellType = getAttr(reader, "t");
                        String styleAttr = getAttr(reader, "s");
                        currentStyleIndex = -1;
                        if (styleAttr != null && !styleAttr.isBlank()) {
                            try {
                                currentStyleIndex = Integer.parseInt(styleAttr.trim());
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        currentCellHasFormula = false;
                        currentValue = new StringBuilder();
                        currentFormula = new StringBuilder();
                        currentInlineStr = new StringBuilder();
                        inValue = false;
                        inFormula = false;
                        inInlineText = false;
                        inPhonetic = false;
                    } else if ("f".equals(local) && currentRowCells != null) {
                        currentCellHasFormula = true;
                        inFormula = true;
                    } else if ("v".equals(local) && currentRowCells != null) {
                        inValue = true;
                    } else if ("rPh".equals(local) && currentRowCells != null) {
                        inPhonetic = true;
                    } else if ("t".equals(local) && currentRowCells != null && !inPhonetic) {
                        inInlineText = true;
                    }
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    if (inFormula && currentFormula != null) {
                        currentFormula.append(reader.getText());
                    } else if (inValue && currentValue != null) {
                        currentValue.append(reader.getText());
                    } else if (inInlineText && currentInlineStr != null) {
                        currentInlineStr.append(reader.getText());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("f".equals(local)) {
                        inFormula = false;
                    } else if ("v".equals(local)) {
                        inValue = false;
                    } else if ("t".equals(local)) {
                        inInlineText = false;
                    } else if ("rPh".equals(local)) {
                        inPhonetic = false;
                    } else if ("c".equals(local) && currentRowCells != null && currentColIndex >= 0) {
                        ParsedCell parsedCell = buildParsedCell(
                                currentRowNum,
                                currentColIndex,
                                currentColLetter,
                                currentCellType,
                                currentStyleIndex,
                                currentCellHasFormula,
                                currentValue != null ? currentValue.toString() : "",
                                currentFormula != null ? currentFormula.toString() : "",
                                currentInlineStr != null ? currentInlineStr.toString() : "",
                                sharedStrings,
                                dateStyleIndices
                        );
                        currentRowCells.put(currentColIndex, parsedCell);
                    } else if ("row".equals(local) && currentRowCells != null) {
                        ParsedRow row = new ParsedRow(currentRowNum, currentRowCells);
                        if (currentRowNum == 1 && headerRow == null) {
                            headerRow = row;
                        } else if (headerRow == null && !row.isCompletelyBlank()) {
                            headerRow = row;
                        } else if (!row.isCompletelyBlank()) {
                            dataRows.add(row);
                            if (dataRows.size() > props.maxRows()) {
                                throw ApiException.badRequest("Số dòng dữ liệu trong tệp vượt quá giới hạn tối đa cho phép ("
                                        + props.maxRows() + " dòng)");
                            }
                        }
                        currentRowCells = null;
                    }
                }
            }
            reader.close();
        } catch (XMLStreamException ex) {
            throw ApiException.badRequest("Nội dung XML của sheet '" + sheetName + "' không hợp lệ hoặc bị hỏng");
        }

        return new ParsedSheet(sheetName, headerRow, dataRows);
    }

    private static ParsedCell buildParsedCell(
            int rowNum,
            int colIndex,
            String colLetter,
            String cellType,
            int styleIndex,
            boolean hasFormula,
            String valStr,
            String formulaStr,
            String inlineStr,
            List<String> sharedStrings,
            Set<Integer> dateStyleIndices
    ) {
        if (hasFormula || "str".equals(cellType)) {
            String fText = (formulaStr != null && !formulaStr.isBlank()) ? "=" + formulaStr : valStr;
            return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.FORMULA, valStr, fText);
        }
        if ("e".equals(cellType)) {
            return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.ERROR, valStr, null);
        }
        if ("b".equals(cellType)) {
            String boolText = ("1".equals(valStr.trim()) || "true".equalsIgnoreCase(valStr.trim())) ? "TRUE" : "FALSE";
            return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.BOOLEAN, boolText, null);
        }
        if ("s".equals(cellType)) {
            String trimmed = valStr.trim();
            if (trimmed.isEmpty()) {
                return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.BLANK, "", null);
            }
            try {
                int idx = Integer.parseInt(trimmed);
                if (idx < 0 || idx >= sharedStrings.size()) {
                    throw ApiException.badRequest("Tham chiếu chuỗi (sharedString) không hợp lệ tại dòng " + rowNum + " cột " + colLetter);
                }
                String resolved = sharedStrings.get(idx);
                if (resolved != null && resolved.trim().startsWith("=")) {
                    return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.FORMULA, resolved, resolved.trim());
                }
                if (resolved == null || resolved.isBlank()) {
                    return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.BLANK, "", null);
                }
                return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.STRING, resolved, null);
            } catch (NumberFormatException ex) {
                throw ApiException.badRequest("Chỉ số chuỗi (sharedString) không hợp lệ tại dòng " + rowNum + " cột " + colLetter);
            }
        }
        if ("inlineStr".equals(cellType)) {
            String text = !inlineStr.isEmpty() ? inlineStr : valStr;
            if (text != null && text.trim().startsWith("=")) {
                return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.FORMULA, text, text.trim());
            }
            if (text == null || text.isBlank()) {
                return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.BLANK, "", null);
            }
            return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.STRING, text, null);
        }

        // Mặc định hoặc t="n"
        String raw = !valStr.isEmpty() ? valStr : inlineStr;
        if (raw == null || raw.isBlank()) {
            return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.BLANK, "", null);
        }
        if (raw.trim().startsWith("=")) {
            return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.FORMULA, raw, raw.trim());
        }
        if (styleIndex >= 0 && dateStyleIndices.contains(styleIndex)) {
            return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.DATE, raw.trim(), null);
        }
        return new ParsedCell(rowNum, colIndex, colLetter, CellValueKind.NUMERIC, raw.trim(), null);
    }

    private static void checkDeadline(long deadlineNanos, long timeoutMs) {
        if (System.nanoTime() > deadlineNanos) {
            throw ApiException.badRequest("Thời gian xử lý tệp .xlsx vượt quá giới hạn cho phép (" + timeoutMs + " ms)");
        }
    }

    private static String getAttr(XMLStreamReader reader, String name) {
        String v = reader.getAttributeValue(null, name);
        if (v != null) {
            return v;
        }
        return getAttrByLocalName(reader, name);
    }

    private static String getAttrByLocalName(XMLStreamReader reader, String localName) {
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (localName.equals(reader.getAttributeLocalName(i))) {
                return reader.getAttributeValue(i);
            }
        }
        return null;
    }

    public static String extractColLetter(String cellRef) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cellRef.length(); i++) {
            char c = cellRef.charAt(i);
            if (Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
            } else {
                break;
            }
        }
        return sb.isEmpty() ? "A" : sb.toString();
    }

    public static int colLetterToIndex(String colLetter) {
        int result = 0;
        for (int i = 0; i < colLetter.length(); i++) {
            char c = Character.toUpperCase(colLetter.charAt(i));
            if (c >= 'A' && c <= 'Z') {
                result = result * 26 + (c - 'A' + 1);
            }
        }
        return Math.max(0, result - 1);
    }

    public static String indexToColLetter(int colIndex) {
        int n = colIndex + 1;
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            int rem = (n - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return sb.isEmpty() ? "A" : sb.toString();
    }

    private static void writeZipEntry(ZipOutputStream zip, String name, String xmlContent) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(xmlContent.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String buildContentTypesXml(int sheetCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n");
        sb.append("  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n");
        sb.append("  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n");
        sb.append("  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n");
        sb.append("  <Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>\n");
        for (int i = 1; i <= sheetCount; i++) {
            sb.append("  <Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n");
        }
        sb.append("</Types>");
        return sb.toString();
    }

    private static String buildRootRelsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;
    }

    private static String buildWorkbookXml(List<String> sheetNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n");
        sb.append("  <sheets>\n");
        for (int i = 0; i < sheetNames.size(); i++) {
            int id = i + 1;
            sb.append("    <sheet name=\"").append(escapeXml(sheetNames.get(i)))
                    .append("\" sheetId=\"").append(id)
                    .append("\" r:id=\"rId").append(id).append("\"/>\n");
        }
        sb.append("  </sheets>\n");
        sb.append("</workbook>");
        return sb.toString();
    }

    private static String buildWorkbookRelsXml(int sheetCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n");
        for (int i = 1; i <= sheetCount; i++) {
            sb.append("  <Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" ")
                    .append("Target=\"worksheets/sheet").append(i).append(".xml\"/>\n");
        }
        sb.append("  <Relationship Id=\"rId").append(sheetCount + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" ")
                .append("Target=\"styles.xml\"/>\n");
        sb.append("</Relationships>");
        return sb.toString();
    }

    private static String buildStylesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="2">
                    <font><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><sz val="11"/><name val="Calibri"/></font>
                  </fonts>
                  <fills count="2">
                    <fill><patternFill patternType="none"/></fill>
                    <fill><patternFill patternType="gray125"/></fill>
                  </fills>
                  <borders count="1">
                    <border><left/><right/><top/><bottom/><diagonal/></border>
                  </borders>
                  <cellStyleXfs count="1">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
                  </cellStyleXfs>
                  <cellXfs count="2">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                  </cellXfs>
                </styleSheet>
                """;
    }

    private static String buildSheetXml(List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
        sb.append("  <cols>\n");
        sb.append("    <col min=\"1\" max=\"1\" width=\"18\" customWidth=\"1\"/>\n");
        sb.append("    <col min=\"2\" max=\"2\" width=\"28\" customWidth=\"1\"/>\n");
        sb.append("    <col min=\"3\" max=\"4\" width=\"16\" customWidth=\"1\"/>\n");
        sb.append("  </cols>\n");
        sb.append("  <sheetData>\n");

        if (rows != null) {
            for (int r = 0; r < rows.size(); r++) {
                int rowNum = r + 1;
                List<Object> cells = rows.get(r);
                sb.append("    <row r=\"").append(rowNum).append("\">\n");
                if (cells != null) {
                    for (int c = 0; c < cells.size(); c++) {
                        String ref = indexToColLetter(c) + rowNum;
                        String styleAttr = (rowNum == 1) ? " s=\"1\"" : "";
                        Object val = cells.get(c);
                        if (val == null) {
                            sb.append("      <c r=\"").append(ref).append("\"").append(styleAttr).append("/>\n");
                        } else if (val instanceof Number num) {
                            sb.append("      <c r=\"").append(ref).append("\" t=\"n\"").append(styleAttr)
                                    .append("><v>").append(num).append("</v></c>\n");
                        } else if (val instanceof Boolean bool) {
                            sb.append("      <c r=\"").append(ref).append("\" t=\"b\"").append(styleAttr)
                                    .append("><v>").append(bool ? "1" : "0").append("</v></c>\n");
                        } else {
                            String str = String.valueOf(val);
                            if (str.startsWith("FORMULA:")) {
                                String fExpr = str.substring("FORMULA:".length());
                                sb.append("      <c r=\"").append(ref).append("\"").append(styleAttr)
                                        .append("><f>").append(escapeXml(fExpr)).append("</f><v>0</v></c>\n");
                            } else {
                                sb.append("      <c r=\"").append(ref).append("\" t=\"inlineStr\"").append(styleAttr)
                                        .append("><is><t>").append(escapeXml(str)).append("</t></is></c>\n");
                            }
                        }
                    }
                }
                sb.append("    </row>\n");
            }
        }

        sb.append("  </sheetData>\n");
        sb.append("</worksheet>");
        return sb.toString();
    }

    private static String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
