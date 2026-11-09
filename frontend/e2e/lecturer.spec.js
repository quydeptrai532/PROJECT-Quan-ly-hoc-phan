import { expect, test } from "@playwright/test";
import { readFile } from "node:fs/promises";
import { login } from "./helpers";

test.describe("Giảng viên", () => {
  test("xem tổng quan, lớp phụ trách và nhập điểm", async ({ page }) => {
    await login(page, "gv01", "123456");
    await expect(page).toHaveURL(/\/tong-quan$/);
    await expect(page.getByRole("main").getByText("Không gian giảng dạy")).toBeVisible();
    await expect(page.getByText("Lớp phụ trách")).toBeVisible();

    await page.goto("/lop-cua-toi");
    await expect(page.getByRole("heading", { name: "Lớp học phần phụ trách" })).toBeVisible();
    await expect(page.getByText("INT303.01", { exact: true }).first()).toBeVisible();

    await page.getByRole("link", { name: /Nhập điểm & xem danh sách/ }).first().click();
    await expect(page.getByText("Danh sách sinh viên")).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Mã SV" })).toBeVisible();

    await page.goto("/lop-cua-toi/thoi-khoa-bieu");
    await expect(page.getByRole("heading", { name: "Thời khóa biểu giảng dạy" })).toBeVisible();
  });
    test("xuất danh sách sinh viên của lớp ra CSV và chặn lớp không phụ trách", async ({ page }) => {
        await login(page, "gv01", "123456");
        const token = await page.evaluate(() => localStorage.getItem("token"));
        const headers = { Authorization: `Bearer ${token}` };
        const lopResponse = await page.request.get("/api/giang-vien/me/lop-hoc-phan", { headers });
        const lop = (await lopResponse.json()).find((item) => item.ma === "INT303.01");
        expect(lop).toBeTruthy();

        await page.goto(`/lop-cua-toi/${lop.id}/nhap-diem`);
        await expect(page.getByText("Danh sách sinh viên")).toBeVisible();
        await expect(page.locator("tbody tr").first()).toBeVisible();
        const soDongTrenManHinh = await page.locator("tbody tr").count();
        expect(soDongTrenManHinh).toBeGreaterThan(0);

        const [download] = await Promise.all([
            page.waitForEvent("download"),
            page.getByRole("button", { name: "Xuất CSV" }).click(),
        ]);
        expect(download.suggestedFilename()).toBe("danh-sach-sinh-vien-INT303.01.csv");

        const noiDung = await readFile(await download.path(), "utf8");
        expect(noiDung.startsWith("\uFEFF")).toBe(true);
        const dong = noiDung.slice(1).split("\r\n").filter(Boolean);
        expect(dong[0]).toBe('"STT","Mã sinh viên","Họ tên","Lớp hành chính","Email"');
        expect(dong.length - 1).toBe(soDongTrenManHinh);
        await expect(page.getByText(/Đã xuất danh sách \d+ sinh viên ra file CSV/)).toBeVisible();

        // Giảng viên khác không được xuất danh sách lớp không phụ trách.
        await page.evaluate(() => localStorage.clear());
        await login(page, "gv02", "123456");
        const tokenKhac = await page.evaluate(() => localStorage.getItem("token"));
        const biChan = await page.request.get(`/api/giang-vien/lop-hoc-phan/${lop.id}/sinh-vien/csv`, {
            headers: { Authorization: `Bearer ${tokenKhac}` },
        });
        expect(biChan.status()).toBe(403);
    });
});
