"use client";

import { useEffect, useState } from "react";
import { api, Account } from "../../../lib/api";
import { IconImport } from "../../../lib/icons";

export default function ImportPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [accountId, setAccountId] = useState<number | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<{ ok: boolean; text: string } | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.listAccounts().then((accts) => {
      setAccounts(accts);
      if (accts.length > 0) setAccountId(accts[0].id);
    }).catch((e) => setResult({ ok: false, text: `Couldn't load accounts: ${e.message}` }));
  }, []);

  const handleImport = async () => {
    if (!file || !accountId) return;
    setBusy(true);
    setResult(null);
    try {
      const res = await api.importCsv(accountId, file);
      await api.reindex();
      setResult({ ok: true, text: `Imported ${res.imported} transaction${res.imported === 1 ? "" : "s"}. ${res.errors?.length ?? 0} row${res.errors?.length === 1 ? "" : "s"} had errors.` });
    } catch (e: any) {
      setResult({ ok: false, text: `Import failed: ${e.message}` });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Import CSV</h1>
          <p>Bring in a bank export. We'll re-index your AI assistant automatically.</p>
        </div>
      </div>

      <div className="card">
        <p>Expected columns: <code>date, description, amount</code> (optional: <code>merchant, currency</code>). Dates like <code>YYYY-MM-DD</code>, <code>DD/MM/YYYY</code>, <code>MM/DD/YYYY</code>, or <code>DD.MM.YYYY</code> are all accepted.</p>

        {accounts.length === 0 ? (
          <div className="alert alert-info">
            You need an account before importing — create one on the <a href="/transactions">Transactions</a> page first.
          </div>
        ) : (
          <>
            <div className="field-row" style={{ marginBottom: 14 }}>
              <div className="field">
                <label>Account</label>
                <select value={accountId ?? ""} onChange={(e) => setAccountId(Number(e.target.value))}>
                  {accounts.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
                </select>
              </div>
              <div className="field" style={{ flex: 2 }}>
                <label>CSV file</label>
                <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
              </div>
              <button onClick={handleImport} disabled={!file || busy}>
                <IconImport /> {busy ? "Importing…" : "Import"}
              </button>
            </div>
          </>
        )}
        {result && <div className={`alert ${result.ok ? "alert-info" : "alert-danger"}`}>{result.text}</div>}
      </div>
    </div>
  );
}
