export async function seedUserSession(page, role = "USER") {
  await page.addInitScript(([userRole]) => {
    sessionStorage.setItem("segroup8_force_login_checked", "1");
    localStorage.setItem("segroup8_token", "e2e-token");
    localStorage.setItem(
      "segroup8_user",
      JSON.stringify({
        id: userRole === "ADMIN" ? 2 : 1,
        username: userRole === "ADMIN" ? "admin1" : "buyer1",
        nickname: userRole === "ADMIN" ? "管理员1" : "买家1",
        role: userRole
      })
    );
  }, [role]);
}

