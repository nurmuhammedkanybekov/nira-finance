"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "../../lib/api";
import { ThemeToggle } from "../../components/ThemeToggle";

export default function LoginPage() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const router = useRouter();

  const submit = async () => {
    setError(null);
    setBusy(true);
    try {
      const res = mode === "login"
        ? await api.login(email, password)
        : await api.register(email, password, displayName);
      localStorage.setItem("token", res.token);
      localStorage.setItem("displayName", res.displayName);
      router.push("/dashboard");
    } catch (e: any) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-toggle"><ThemeToggle /></div>
      <div className="auth-card">
        <div className="auth-brand">
          <div className="sidebar-mark" />
          <span style={{ fontWeight: 700, fontSize: 17 }}>Nira Finance</span>
        </div>
        <div className="card">
          <h2 style={{ marginBottom: 2 }}>{mode === "login" ? "Welcome back" : "Create your account"}</h2>
          <p style={{ marginBottom: 18, fontSize: 13 }}>
            {mode === "login" ? "Log in to see your dashboard." : "Takes less than a minute."}
          </p>

          <div className="section-gap">
            {mode === "register" && (
              <div className="field">
                <label>Display name</label>
                <input placeholder="Ada Lovelace" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
              </div>
            )}
            <div className="field">
              <label>Email</label>
              <input type="email" placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="field">
              <label>Password</label>
              <input
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && submit()}
              />
            </div>
          </div>

          {error && <div className="alert alert-danger" style={{ marginTop: 14, marginBottom: 0 }}>{error}</div>}

          <button onClick={submit} disabled={busy} style={{ width: "100%", marginTop: 18 }}>
            {busy ? "Please wait…" : mode === "login" ? "Log in" : "Sign up"}
          </button>

          <p className="auth-switch">
            {mode === "login" ? (
              <>No account? <a onClick={() => setMode("register")} style={{ cursor: "pointer" }}>Sign up</a></>
            ) : (
              <>Have an account? <a onClick={() => setMode("login")} style={{ cursor: "pointer" }}>Log in</a></>
            )}
          </p>
        </div>
      </div>
    </div>
  );
}
