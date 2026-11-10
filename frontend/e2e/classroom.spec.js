import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test.describe("Quản lý phòng học (QLSV-80)", () => {
  test("xem danh sách phòng học, thống kê và bộ lọc", async ({ page }) => {
    await login(page, "admin", "admin123");
    await page.goto("/quan-tri/phong-hoc");

    await expect(page.getByRole("heading", { name: "Quản lý phòng học" })).toBeVisible();
    await expect(page.locator(".stat-card").getByText("Tổng số phòng")).toBeVisible();
    await expect(page.locator(".stat-card").getByText("Đang hoạt động")).toBeVisible();
    await expect(page.locator(".stat-card").getByText("Đang bảo trì")).toBeVisible();
    await expect(page.locator(".stat-card").getByText("Ngừng sử dụng")).toBeVisible();
    await expect(page.locator(".stat-card").getByText("Tổng sức chứa")).toBeVisible();

    // Verify filter elements
    await expect(page.getByPlaceholder("Tìm theo mã, tên phòng, tòa nhà, thiết bị...")).toBeVisible();
  });

  test("thêm mới, cập nhật trạng thái và xóa phòng học", async ({ page }) => {
    await login(page, "admin", "admin123");
    await page.goto("/quan-tri/phong-hoc");

    const maPhong = `E2E${Date.now().toString().slice(-5)}`;

    // 1. Thêm phòng học
    await page.getByRole("button", { name: "Thêm phòng học" }).click();
    await page.getByLabel("Mã phòng học").fill(maPhong);
    await page.getByLabel("Tên phòng học").fill(`Phòng kiểm thử ${maPhong}`);
    await page.getByLabel("Tòa nhà").fill("Nhà E");
    await page.getByLabel("Tầng").fill("3");
    await page.getByLabel("Sức chứa (chỗ)").fill("45");
    await page.getByRole("button", { name: "Tạo phòng học" }).click();

    // Chờ thông báo thành công hoặc phòng xuất hiện
    await expect(page.getByText(`Đã thêm mới phòng học ${maPhong}`)).toBeVisible();

    // 2. Tìm kiếm phòng vừa tạo
    await page.getByPlaceholder("Tìm theo mã, tên phòng, tòa nhà, thiết bị...").fill(maPhong);
    const row = page.locator("tr").filter({ hasText: maPhong });
    await expect(row).toBeVisible();
    await expect(row.getByText("45 chỗ")).toBeVisible();

    // 3. Đổi trạng thái nhanh sang Bảo trì
    await row.getByTitle("Đổi trạng thái phòng học").click();
    const statusModal = page.getByRole("dialog");
    await expect(statusModal.getByRole("heading", { name: `Cập nhật trạng thái: Phòng ${maPhong}` })).toBeVisible();
    await statusModal.locator("select").selectOption("BAO_TRI");
    await statusModal.locator("textarea").fill("Thay bóng máy chiếu");
    await statusModal.getByRole("button", { name: "Cập nhật trạng thái" }).click();

    // Kiểm tra badge bảo trì
    await expect(row.getByRole("button", { name: "Bảo trì" })).toBeVisible();

    // 4. Xóa phòng vừa tạo (chưa có lớp nào xếp lịch)
    await row.getByTitle("Xóa phòng học").click();
    const confirmModal = page.getByRole("dialog");
    await expect(confirmModal.getByRole("heading", { name: "Xác nhận xóa phòng học" })).toBeVisible();
    await confirmModal.getByRole("button", { name: "Xóa phòng" }).click();

    await expect(page.getByText(`Đã xóa phòng học ${maPhong}`)).toBeVisible();
  });
});
