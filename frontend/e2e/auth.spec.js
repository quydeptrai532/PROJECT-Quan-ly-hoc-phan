import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test.describe("Xác thực", () => {
  test("hiển thị trang đăng nhập", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByRole("heading", { name: "Đăng nhập" })).toBeVisible();
    await expect(page.locator("#username")).toBeVisible();
    await expect(page.locator("#password")).toBeVisible();
    await expect(page.getByRole("button", { name: "Đăng nhập", exact: true })).toBeVisible();
  });

  test("đăng nhập sai hiển thị thông báo lỗi", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Tên đăng nhập", { exact: true }).fill("sv01");
    await page.getByLabel("Mật khẩu", { exact: true }).fill("sai-mat-khau");
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await expect(page.getByText("Tên đăng nhập hoặc mật khẩu không đúng")).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test("sinh viên đăng nhập vào trang tổng quan", async ({ page }) => {
    await login(page, "sv01", "123456");
    await expect(page).toHaveURL(/\/tong-quan$/);
    await expect(page.getByRole("main").getByText("Không gian học tập")).toBeVisible();
    await expect(page.getByText("Học phần đã đăng ký")).toBeVisible();
  });

  test("chưa đăng nhập bị chuyển về trang login", async ({ page }) => {
    await page.goto("/diem");
    await expect(page).toHaveURL(/\/login$/);
  });

  test("cập nhật họ tên trong hồ sơ cá nhân", async ({ page }) => {
    await login(page, "sv04", "123456");
    await page.goto("/ho-so");

    const hoTen = page.getByLabel(/Họ tên/).first();
    await expect(hoTen).toHaveValue("Vũ Ngọc Lan");
    await hoTen.fill("Vũ Ngọc Lan E2E");

    let saved = page.waitForResponse(
      (response) =>
        response.url().includes("/api/auth/me") &&
        response.request().method() === "PUT" &&
        response.status() === 200
    );
    await page.getByRole("button", { name: "Lưu thay đổi" }).click();
    await saved;
    await expect(page.getByText("Cập nhật hồ sơ thành công")).toBeVisible();
    await expect(page.getByRole("main").getByText("Vũ Ngọc Lan E2E")).toBeVisible();

    await hoTen.fill("Vũ Ngọc Lan");
    saved = page.waitForResponse(
      (response) =>
        response.url().includes("/api/auth/me") &&
        response.request().method() === "PUT" &&
        response.status() === 200
    );
    await page.getByRole("button", { name: "Lưu thay đổi" }).click();
    await saved;
  });
});
