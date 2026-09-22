"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ThemeToggle } from "./ThemeToggle";
import {
  IconDashboard, IconTransactions, IconImport, IconChat, IconInvestments, IconBudgets, IconLogout,
} from "../lib/icons";

const NAV = [
  { href: "/dashboard", label: "Dashboard", icon: IconDashboard },
  { href: "/transactions", label: "Transactions", icon: IconTransactions },
  { href: "/budgets", label: "Budgets", icon: IconBudgets },
  { href: "/investments", label: "Investments", icon: IconInvestments },
  { href: "/import", label: "Import CSV", icon: IconImport },
  { href: "/chat", label: "Ask my finances", icon: IconChat },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [displayName, setDisplayName] = useState<string | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      router.replace("/login");
      return;
    }
    setDisplayName(localStorage.getItem("displayName"));
    setReady(true);
  }, [router]);

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("displayName");
    router.replace("/login");
  };

  if (!ready) return null;

  const initial = (displayName || "?").trim().charAt(0).toUpperCase();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="sidebar-mark" />
          <span>Nira Finance</span>
        </div>
        {NAV.map(({ href, label, icon: Icon }) => (
          <Link key={href} href={href} className={`nav-link${pathname === href ? " active" : ""}`}>
            <Icon />
            {label}
          </Link>
        ))}
        <div className="sidebar-footer">
          <div className="sidebar-user">
            <div className="sidebar-avatar">{initial}</div>
            <span className="sidebar-user-name">{displayName || "Account"}</span>
          </div>
          <button type="button" className="sidebar-logout" onClick={logout}>
            <IconLogout /> Log out
          </button>
        </div>
      </aside>
      <div className="main-col">
        <div className="topbar">
          <ThemeToggle />
        </div>
        <div className="content">{children}</div>
      </div>
    </div>
  );
}
