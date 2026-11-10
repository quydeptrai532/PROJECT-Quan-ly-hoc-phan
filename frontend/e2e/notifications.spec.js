import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test.describe("Trung tâm thông báo", () => {
  test("đăng ký lớp tạo thông báo, đánh dấu đã đọc và mở đúng trang đích", async ({ page }) => {
    await login(page, "sv02", "123456");

    await page.goto("/hoc-phan-cua-toi");
    const existing = page.locator("tr").filter({ hasText: "INT201.01" });
    if (await existing.count()) {
      await existing.getByRole("button", { name: "Hủy", exact: true }).click();
      await page.getByRole("button", { name: "Hủy đăng ký", exact: true }).click();
      await expect(page.getByText(/Đã hủy đăng ký/)).toBeVisible();
    }

    await page.goto("/thong-bao");
    await expect(page.locator(".animate-spin")).toHaveCount(0);
    const clearUnread = page.getByRole("button", { name: "Đánh dấu tất cả đã đọc" });
    if (await clearUnread.isVisible()) {
      await clearUnread.click();
      await expect(clearUnread).toHaveCount(0);
    }

    await page.goto("/dang-ky");
    await page.getByPlaceholder("Tìm theo mã lớp, môn học, giảng viên...").fill("INT201.01");
    const section = page.locator("tr").filter({ hasText: "INT201.01" });
    await expect(section).toBeVisible();
    await section.getByRole("button", { name: "Đăng ký", exact: true }).click();
    await expect(page.getByText(/Đăng ký thành công lớp INT201\.01/)).toBeVisible();

    await page.goto("/thong-bao");
    await expect(page.locator(".animate-spin")).toHaveCount(0);
    const notice = page.locator("article").filter({ hasText: "Đăng ký học phần thành công" })
      .filter({ hasText: "INT201.01" }).first();
    await expect(notice).toBeVisible();
    await expect(notice.getByText("Chưa đọc")).toBeVisible();

    const patchRead = page.waitForResponse(
      (response) =>
        response.url().includes("/api/notifications") &&
        response.url().includes("/read") &&
        response.request().method() === "PATCH" &&
        response.ok()
    );
    await notice.getByRole("button", { name: "Mở nội dung" }).click();
    await patchRead;
    await expect(page).toHaveURL(/\/hoc-phan-cua-toi$/);

    await page.goto("/thong-bao");
    await expect(page.locator(".animate-spin")).toHaveCount(0);
    await page.getByRole("button", { name: /Chưa đọc/ }).click();
    await expect(page.locator(".animate-spin")).toHaveCount(0);
    await expect(page.getByText("Bạn đã xem hết thông báo mới")).toBeVisible();
  });
});
