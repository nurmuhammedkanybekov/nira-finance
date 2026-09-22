import Link from "next/link";
import { ThemeToggle } from "../components/ThemeToggle";
import { IconBudgets, IconChat, IconInvestments, IconTransactions } from "../lib/icons";

export default function Home() {
  return (
    <div className="landing">
      <div className="landing-nav">
        <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
          <div className="sidebar-mark" />
          <span style={{ fontWeight: 700 }}>Nira Finance</span>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <ThemeToggle />
          <Link href="/login"><button className="btn-secondary">Log in</button></Link>
        </div>
      </div>

      <div className="landing-hero">
        <h1>Your finances, actually explained.</h1>
        <p>Track spending, budgets, and investments in one place — then ask an AI assistant plain-English questions about your own money.</p>
        <div className="landing-cta">
          <Link href="/login"><button style={{ padding: "10px 20px" }}>Get started</button></Link>
        </div>

        <div className="feature-grid">
          <div className="feature-card">
            <IconTransactions />
            <h4>Transactions &amp; CSV import</h4>
            <p>Add manually or bulk-import a bank export — auto-categorized where possible.</p>
          </div>
          <div className="feature-card">
            <IconBudgets />
            <h4>Budgets</h4>
            <p>Set monthly limits per category and see exactly where you stand.</p>
          </div>
          <div className="feature-card">
            <IconInvestments />
            <h4>Investments</h4>
            <p>Track holdings, cost basis, and unrealized gain or loss.</p>
          </div>
          <div className="feature-card">
            <IconChat />
            <h4>Ask your finances</h4>
            <p>A RAG-powered assistant that answers using your real transaction history.</p>
          </div>
        </div>
      </div>
    </div>
  );
}
