import { describe, it, expect } from "vitest";
import { loginSchema, registerSchema } from "./validation";

describe("loginSchema", () => {
  it("accepts a valid email and non-empty password", () => {
    const result = loginSchema.safeParse({ email: "person@example.com", password: "anything" });
    expect(result.success).toBe(true);
  });

  it("rejects an invalid email", () => {
    const result = loginSchema.safeParse({ email: "not-an-email", password: "anything" });
    expect(result.success).toBe(false);
  });

  it("rejects an empty password", () => {
    const result = loginSchema.safeParse({ email: "person@example.com", password: "" });
    expect(result.success).toBe(false);
  });
});

describe("registerSchema", () => {
  it("accepts a password meeting all complexity rules", () => {
    const result = registerSchema.safeParse({
      fullName: "Jane Doe",
      email: "jane@example.com",
      password: "SecurePass1",
    });
    expect(result.success).toBe(true);
  });

  // Mirrors the backend's Bean Validation regex (RegisterRequest.java) —
  // keeping both sides' rules in sync matters, since a mismatch means
  // either false client-side rejections or a confusing 400 from the
  // server after the client already said "looks good."
  it.each([
    ["short1A", "too short"],
    ["alllowercase1", "missing uppercase"],
    ["ALLUPPERCASE1", "missing lowercase"],
    ["NoDigitsHere", "missing digit"],
  ])("rejects '%s' (%s)", (password) => {
    const result = registerSchema.safeParse({
      fullName: "Jane Doe",
      email: "jane@example.com",
      password,
    });
    expect(result.success).toBe(false);
  });
});
