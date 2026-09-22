"use client";

import { useEffect, useState } from "react";
import { api, Account, Category, Transaction } from "../../../lib/api";
import { IconEmptyInbox, IconPlus, IconTrash } from "../../../lib/icons";

export default function Transactions() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [error, setError] = useState<string | null>(null);

  const [newAccountName, setNewAccountName] = useState("");
  const [newAccountType, setNewAccountType] = useState<Account["type"]>("CHECKING");
  const [newAccountCurrency, setNewAccountCurrency] = useState("EUR");

  const [accountId, setAccountId] = useState<number | null>(null);
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [merchant, setMerchant] = useState("");
  const [occurredOn, setOccurredOn] = useState(() => new Date().toISOString().slice(0, 10));

  const load = () => {
    api.listTransactions().then(setTransactions).catch((e) => setError(e.message));
    api.listAccounts().then((accts) => {
      setAccounts(accts);
      setAccountId((prev) => prev ?? (accts.length > 0 ? accts[0].id : null));
    }).catch((e) => setError(e.message));
    api.listCategories().then(setCategories).catch(() => {});
  };

  useEffect(load, []);

  const categoryName = (id: number | null) => {
    if (id === null) return "Uncategorized";
    return categories.find((c) => c.id === id)?.name ?? "Uncategorized";
  };

  const createAccount = async () => {
    if (!newAccountName.trim()) return;
    try {
      const acct = await api.createAccount({ name: newAccountName, type: newAccountType, currency: newAccountCurrency });
      setNewAccountName("");
      setAccountId(acct.id);
      load();
    } catch (e: any) {
      setError(e.message);
    }
  };

  const addTransaction = async () => {
    if (!accountId || !amount || !occurredOn) return;
    try {
      await api.createTransaction({
        accountId,
        amount: Number(amount),
        description,
        merchant: merchant || null,
        occurredOn,
        currency: accounts.find((a) => a.id === accountId)?.currency ?? "EUR",
      });
      setAmount("");
      setDescription("");
      setMerchant("");
      load();
    } catch (e: any) {
      setError(e.message);
    }
  };

  const removeTransaction = async (id: number) => {
    try {
      await api.deleteTransaction(id);
      setTransactions((prev) => prev.filter((t) => t.id !== id));
    } catch (e: any) {
      setError(e.message);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Transactions</h1>
          <p>Manage accounts and record income or spending.</p>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card">
        <div className="card-title"><h3>Accounts</h3></div>
        {accounts.length > 0 && (
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 14 }}>
            {accounts.map((a) => (
              <span key={a.id} className="badge badge-neutral">{a.name} · {a.type.replace("_", " ")} · {a.currency}</span>
            ))}
          </div>
        )}
        <div className="field-row">
          <div className="field">
            <label>Account name</label>
            <input placeholder="e.g. Main checking" value={newAccountName} onChange={(e) => setNewAccountName(e.target.value)} />
          </div>
          <div className="field">
            <label>Type</label>
            <select value={newAccountType} onChange={(e) => setNewAccountType(e.target.value as Account["type"])}>
              <option value="CHECKING">Checking</option>
              <option value="SAVINGS">Savings</option>
              <option value="INVESTMENT">Investment</option>
              <option value="CASH">Cash</option>
              <option value="CREDIT_CARD">Credit card</option>
            </select>
          </div>
          <div className="field" style={{ maxWidth: 90 }}>
            <label>Currency</label>
            <input value={newAccountCurrency} onChange={(e) => setNewAccountCurrency(e.target.value)} />
          </div>
          <button onClick={createAccount}><IconPlus /> Create</button>
        </div>
      </div>

      <div className="card">
        <div className="card-title"><h3>Add a transaction</h3></div>
        {accounts.length === 0 ? (
          <p>Create an account above first.</p>
        ) : (
          <div className="field-row">
            <div className="field">
              <label>Account</label>
              <select value={accountId ?? ""} onChange={(e) => setAccountId(Number(e.target.value))}>
                {accounts.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
              </select>
            </div>
            <div className="field" style={{ maxWidth: 150 }}>
              <label>Date</label>
              <input type="date" value={occurredOn} onChange={(e) => setOccurredOn(e.target.value)} />
            </div>
            <div className="field" style={{ maxWidth: 150 }}>
              <label>Amount</label>
              <input placeholder="-42.50" value={amount} onChange={(e) => setAmount(e.target.value)} />
            </div>
            <div className="field">
              <label>Description</label>
              <input placeholder="e.g. Grocery run" value={description} onChange={(e) => setDescription(e.target.value)} />
            </div>
            <div className="field">
              <label>Merchant</label>
              <input placeholder="e.g. Whole Foods" value={merchant} onChange={(e) => setMerchant(e.target.value)} />
            </div>
            <button onClick={addTransaction}><IconPlus /> Add</button>
          </div>
        )}
      </div>

      <div className="card">
        {transactions.length === 0 ? (
          <div className="empty-state">
            <IconEmptyInbox />
            <div className="empty-state-title">No transactions yet</div>
            <p>Add one above, or import a CSV from your bank.</p>
          </div>
        ) : (
          <table>
            <thead>
              <tr><th>Date</th><th>Description</th><th>Merchant</th><th className="num">Amount</th><th>Category</th><th /></tr>
            </thead>
            <tbody>
              {transactions.map((t) => (
                <tr key={t.id}>
                  <td>{t.occurredOn}</td>
                  <td>{t.description || "—"}</td>
                  <td>{t.merchant ?? "—"}</td>
                  <td className={`num ${t.amount < 0 ? "amount-negative" : "amount-positive"}`}>
                    {t.amount > 0 ? "+" : ""}{t.amount.toFixed(2)} {t.currency}
                  </td>
                  <td>
                    <span className="badge badge-neutral">{categoryName(t.categoryId)}</span>
                    {t.aiCategorized && <span className="badge badge-accent" style={{ marginLeft: 6 }}>AI</span>}
                  </td>
                  <td>
                    <button className="btn-ghost icon-btn btn-sm" onClick={() => removeTransaction(t.id)} aria-label="Delete transaction">
                      <IconTrash />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
