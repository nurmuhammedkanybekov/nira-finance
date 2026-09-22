"use client";

import { useEffect, useState } from "react";
import { api, BudgetStatus, Category } from "../../../lib/api";
import { IconBudgets, IconPlus } from "../../../lib/icons";

function fmtMoney(n: number) {
  return new Intl.NumberFormat(undefined, { style: "currency", currency: "EUR", maximumFractionDigits: 2 }).format(n);
}

function currentMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

export default function BudgetsPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [statuses, setStatuses] = useState<BudgetStatus[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [month] = useState(currentMonth());

  const [newCategoryName, setNewCategoryName] = useState("");
  const [newCategoryIncome, setNewCategoryIncome] = useState(false);

  const [budgetCategoryId, setBudgetCategoryId] = useState<number | null>(null);
  const [budgetLimit, setBudgetLimit] = useState("");

  const load = () => {
    api.listCategories().then((cats) => {
      setCategories(cats);
      setBudgetCategoryId((prev) => prev ?? (cats.length > 0 ? cats[0].id : null));
    }).catch((e) => setError(e.message));
    api.budgetStatus(month).then(setStatuses).catch((e) => setError(e.message));
  };

  useEffect(load, [month]);

  const categoryName = (id: number) => categories.find((c) => c.id === id)?.name ?? `#${id}`;

  const createCategory = async () => {
    if (!newCategoryName.trim()) return;
    try {
      await api.createCategory({ name: newCategoryName, income: newCategoryIncome });
      setNewCategoryName("");
      load();
    } catch (e: any) {
      setError(e.message);
    }
  };

  const submitBudget = async () => {
    if (!budgetCategoryId || !budgetLimit) return;
    try {
      await api.setBudget(budgetCategoryId, month, Number(budgetLimit));
      setBudgetLimit("");
      load();
    } catch (e: any) {
      setError(e.message);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Budgets</h1>
          <p>Monthly spending limits per category — {month}.</p>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card">
        <div className="card-title"><h3>Categories</h3></div>
        {categories.length > 0 && (
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 14 }}>
            {categories.map((c) => (
              <span key={c.id} className={`badge ${c.income ? "badge-success" : "badge-neutral"}`}>{c.name}</span>
            ))}
          </div>
        )}
        <div className="field-row">
          <div className="field">
            <label>New category</label>
            <input placeholder="e.g. Groceries" value={newCategoryName} onChange={(e) => setNewCategoryName(e.target.value)} />
          </div>
          <div className="field" style={{ maxWidth: 140 }}>
            <label>Type</label>
            <select value={newCategoryIncome ? "income" : "expense"} onChange={(e) => setNewCategoryIncome(e.target.value === "income")}>
              <option value="expense">Expense</option>
              <option value="income">Income</option>
            </select>
          </div>
          <button onClick={createCategory}><IconPlus /> Create</button>
        </div>
      </div>

      <div className="card">
        <div className="card-title"><h3>Set a budget</h3></div>
        {categories.length === 0 ? (
          <p>Create a category above first.</p>
        ) : (
          <div className="field-row">
            <div className="field">
              <label>Category</label>
              <select value={budgetCategoryId ?? ""} onChange={(e) => setBudgetCategoryId(Number(e.target.value))}>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <div className="field" style={{ maxWidth: 160 }}>
              <label>Monthly limit</label>
              <input placeholder="500" value={budgetLimit} onChange={(e) => setBudgetLimit(e.target.value)} />
            </div>
            <button onClick={submitBudget}><IconPlus /> Set budget</button>
          </div>
        )}
      </div>

      <div className="card">
        {statuses.length === 0 ? (
          <div className="empty-state">
            <IconBudgets />
            <div className="empty-state-title">No budgets set for {month}</div>
            <p>Set one above to start tracking against it.</p>
          </div>
        ) : (
          <table>
            <thead>
              <tr><th>Category</th><th className="num">Spent</th><th className="num">Limit</th><th className="num">Remaining</th><th>Status</th></tr>
            </thead>
            <tbody>
              {statuses.map((s) => {
                const remaining = s.limit - s.spent;
                const pct = s.limit > 0 ? Math.min(100, (s.spent / s.limit) * 100) : 0;
                return (
                  <tr key={s.categoryId}>
                    <td>{categoryName(s.categoryId)}</td>
                    <td className="num">{fmtMoney(s.spent)}</td>
                    <td className="num">{fmtMoney(s.limit)}</td>
                    <td className={`num ${remaining < 0 ? "amount-negative" : "amount-positive"}`}>{fmtMoney(remaining)}</td>
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                        <div style={{ width: 60, height: 6, borderRadius: 3, background: "var(--surface-2)", overflow: "hidden" }}>
                          <div style={{ width: `${pct}%`, height: "100%", background: s.overBudget ? "var(--danger)" : "var(--success)" }} />
                        </div>
                        <span className={`badge ${s.overBudget ? "badge-danger" : "badge-success"}`}>
                          {s.overBudget ? "Over budget" : "On track"}
                        </span>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
