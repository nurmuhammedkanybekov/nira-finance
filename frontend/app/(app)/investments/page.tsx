"use client";

import { useEffect, useState } from "react";
import { api, Account, Holding, PortfolioSummary } from "../../../lib/api";
import { IconEmptyInbox, IconInvestments, IconPlus, IconTrash } from "../../../lib/icons";

function fmtMoney(n: number) {
  return new Intl.NumberFormat(undefined, { style: "currency", currency: "USD", maximumFractionDigits: 2 }).format(n);
}
function fmtPercent(n: number) {
  return `${n >= 0 ? "+" : ""}${n.toFixed(2)}%`;
}

export default function InvestmentsPage() {
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [summary, setSummary] = useState<PortfolioSummary | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [error, setError] = useState<string | null>(null);

  const [accountId, setAccountId] = useState<number | null>(null);
  const [ticker, setTicker] = useState("");
  const [shares, setShares] = useState("");
  const [costBasis, setCostBasis] = useState("");
  const [priceDrafts, setPriceDrafts] = useState<Record<number, string>>({});

  const load = () => {
    api.listHoldings().then(setHoldings).catch((e) => setError(e.message));
    api.holdingsSummary().then(setSummary).catch(() => {});
    api.listAccounts().then((accts) => {
      setAccounts(accts);
      setAccountId((prev) => prev ?? (accts.length > 0 ? accts[0].id : null));
    }).catch((e) => setError(e.message));
  };

  useEffect(load, []);

  const accountName = (id: number) => accounts.find((a) => a.id === id)?.name ?? `#${id}`;

  const addHolding = async () => {
    if (!accountId || !ticker || !shares || !costBasis) return;
    try {
      await api.createHolding({ accountId, ticker, shares: Number(shares), costBasis: Number(costBasis) });
      setTicker(""); setShares(""); setCostBasis("");
      load();
    } catch (e: any) {
      setError(e.message);
    }
  };

  const savePrice = async (id: number) => {
    const draft = priceDrafts[id];
    if (!draft) return;
    try {
      await api.updateHoldingPrice(id, Number(draft));
      setPriceDrafts((prev) => { const next = { ...prev }; delete next[id]; return next; });
      load();
    } catch (e: any) {
      setError(e.message);
    }
  };

  const removeHolding = async (id: number) => {
    try {
      await api.deleteHolding(id);
      setHoldings((prev) => prev.filter((h) => h.id !== id));
    } catch (e: any) {
      setError(e.message);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Investments</h1>
          <p>Manual portfolio tracking — no live market data connected yet.</p>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {summary && holdings.length > 0 && (
        <div className="stat-grid">
          <div className="stat-tile">
            <div className="stat-label">Market value</div>
            <div className="stat-value">{fmtMoney(summary.totalMarketValue)}</div>
          </div>
          <div className="stat-tile">
            <div className="stat-label">Cost basis</div>
            <div className="stat-value">{fmtMoney(summary.totalCostBasis)}</div>
          </div>
          <div className="stat-tile">
            <div className="stat-label">Unrealized gain/loss</div>
            <div className="stat-value" style={{ color: summary.totalGainLoss >= 0 ? "var(--success)" : "var(--danger)" }}>
              {fmtMoney(summary.totalGainLoss)}
            </div>
            <div className="stat-sub" style={{ color: summary.totalGainLoss >= 0 ? "var(--success)" : "var(--danger)" }}>
              {fmtPercent(summary.totalGainLossPercent)}
            </div>
          </div>
        </div>
      )}

      <div className="card">
        <div className="card-title"><h3>Add a holding</h3></div>
        {accounts.length === 0 ? (
          <p>Create an account on the <a href="/transactions">Transactions</a> page first.</p>
        ) : (
          <div className="field-row">
            <div className="field">
              <label>Account</label>
              <select value={accountId ?? ""} onChange={(e) => setAccountId(Number(e.target.value))}>
                {accounts.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
              </select>
            </div>
            <div className="field" style={{ maxWidth: 120 }}>
              <label>Ticker</label>
              <input placeholder="AAPL" value={ticker} onChange={(e) => setTicker(e.target.value.toUpperCase())} />
            </div>
            <div className="field" style={{ maxWidth: 130 }}>
              <label>Shares</label>
              <input placeholder="10" value={shares} onChange={(e) => setShares(e.target.value)} />
            </div>
            <div className="field" style={{ maxWidth: 160 }}>
              <label>Total cost basis</label>
              <input placeholder="1500.00" value={costBasis} onChange={(e) => setCostBasis(e.target.value)} />
            </div>
            <button onClick={addHolding}><IconPlus /> Add</button>
          </div>
        )}
      </div>

      <div className="card">
        {holdings.length === 0 ? (
          <div className="empty-state">
            <IconEmptyInbox />
            <div className="empty-state-title">No holdings yet</div>
            <p>Add a position above to start tracking your portfolio.</p>
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Ticker</th><th>Account</th><th className="num">Shares</th><th className="num">Cost basis</th>
                <th className="num">Price</th><th className="num">Market value</th><th className="num">Gain/loss</th><th />
              </tr>
            </thead>
            <tbody>
              {holdings.map((h) => (
                <tr key={h.id}>
                  <td><strong>{h.ticker}</strong></td>
                  <td>{accountName(h.accountId)}</td>
                  <td className="num">{h.shares}</td>
                  <td className="num">{fmtMoney(h.costBasis)}</td>
                  <td className="num">
                    <div style={{ display: "flex", gap: 6, justifyContent: "flex-end", alignItems: "center" }}>
                      {h.priceIsEstimated && <span className="badge badge-warning" style={{ fontSize: 10 }}>est.</span>}
                      <input
                        style={{ width: 90, textAlign: "right" }}
                        placeholder={h.currentPrice ? String(h.currentPrice) : "Set price"}
                        value={priceDrafts[h.id] ?? ""}
                        onChange={(e) => setPriceDrafts((prev) => ({ ...prev, [h.id]: e.target.value }))}
                        onKeyDown={(e) => e.key === "Enter" && savePrice(h.id)}
                      />
                      <button className="btn-secondary btn-sm" onClick={() => savePrice(h.id)}>Set</button>
                    </div>
                  </td>
                  <td className="num">{fmtMoney(h.marketValue)}</td>
                  <td className={`num ${h.gainLoss >= 0 ? "amount-positive" : "amount-negative"}`}>
                    {fmtMoney(h.gainLoss)}<br /><span style={{ fontSize: 11.5 }}>{fmtPercent(h.gainLossPercent)}</span>
                  </td>
                  <td>
                    <button className="btn-ghost icon-btn btn-sm" onClick={() => removeHolding(h.id)} aria-label="Delete holding">
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
