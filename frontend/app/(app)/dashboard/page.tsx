"use client";

import { useEffect, useState } from "react";
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from "recharts";
import { api, CategorySummary } from "../../../lib/api";
import { IconEmptyInbox } from "../../../lib/icons";

const SERIES = ["var(--series-1)", "var(--series-2)", "var(--series-3)", "var(--series-4)",
  "var(--series-5)", "var(--series-6)", "var(--series-7)", "var(--series-8)"];

function resolveColor(token: string): string {
  if (typeof window === "undefined") return "#2a78d6";
  return getComputedStyle(document.documentElement).getPropertyValue(token.replace("var(", "").replace(")", "")).trim() || "#2a78d6";
}

function fmtMoney(n: number, currency = "EUR") {
  try {
    return new Intl.NumberFormat(undefined, { style: "currency", currency, maximumFractionDigits: 2 }).format(n);
  } catch {
    return n.toFixed(2);
  }
}

export default function Dashboard() {
  const [data, setData] = useState<CategorySummary[]>([]);
  const [net, setNet] = useState<number | null>(null);
  const [income, setIncome] = useState(0);
  const [spend, setSpend] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [colors, setColors] = useState<string[]>(SERIES);

  useEffect(() => {
    setColors(SERIES.map(resolveColor));

    const now = new Date();
    const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
    const to = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().slice(0, 10);

    api.spendByCategory(from, to).then(setData).catch((e) => setError(e.message));
    api.netForRange(from, to).then(setNet).catch(() => {});
    api.listTransactions().then((txs) => {
      const inRange = txs.filter((t) => t.occurredOn >= from && t.occurredOn <= to);
      setIncome(inRange.filter((t) => t.amount > 0).reduce((s, t) => s + t.amount, 0));
      setSpend(inRange.filter((t) => t.amount < 0).reduce((s, t) => s + Math.abs(t.amount), 0));
    }).catch(() => {});
  }, []);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p>This month at a glance.</p>
        </div>
      </div>

      {error && <div className="alert alert-danger">Couldn't load data: {error}. Is the backend running?</div>}

      <div className="stat-grid">
        <div className="stat-tile">
          <div className="stat-label">Income</div>
          <div className="stat-value">{fmtMoney(income)}</div>
        </div>
        <div className="stat-tile">
          <div className="stat-label">Spend</div>
          <div className="stat-value">{fmtMoney(spend)}</div>
        </div>
        <div className="stat-tile">
          <div className="stat-label">Net</div>
          <div className="stat-value" style={{ color: (net ?? 0) >= 0 ? "var(--success)" : "var(--danger)" }}>
            {net === null ? "—" : fmtMoney(net)}
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-title"><h2>Spend by category</h2></div>
        {data.length === 0 ? (
          <div className="empty-state">
            <IconEmptyInbox />
            <div className="empty-state-title">No spending recorded this month</div>
            <p>Add a transaction or import a CSV to see it broken down here.</p>
          </div>
        ) : (
          <div style={{ height: 300 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <XAxis dataKey="categoryName" stroke="var(--text-muted)" tick={{ fontSize: 12 }} axisLine={{ stroke: "var(--border)" }} tickLine={false} />
                <YAxis stroke="var(--text-muted)" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} width={70}
                  tickFormatter={(v) => fmtMoney(v)} />
                <Tooltip
                  cursor={{ fill: "var(--surface-2)" }}
                  contentStyle={{ background: "var(--surface-1)", border: "1px solid var(--border)", borderRadius: 8, fontSize: 13 }}
                  formatter={(v: number) => fmtMoney(v)}
                />
                <Bar dataKey="total" radius={[4, 4, 0, 0]} maxBarSize={56}>
                  {data.map((_, i) => <Cell key={i} fill={colors[i % colors.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  );
}
