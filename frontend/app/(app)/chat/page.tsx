"use client";

import { useState } from "react";
import { api } from "../../../lib/api";
import { IconChat } from "../../../lib/icons";

interface Turn { question: string; answer: string; sources: string[]; }

export default function ChatPage() {
  const [question, setQuestion] = useState("");
  const [turns, setTurns] = useState<Turn[]>([]);
  const [loading, setLoading] = useState(false);

  const ask = async () => {
    if (!question.trim()) return;
    const q = question;
    setQuestion("");
    setLoading(true);
    try {
      const res = await api.askFinanceChat(q);
      setTurns((prev) => [...prev, { question: q, answer: res.answer, sources: res.sourcesUsed }]);
    } catch (e: any) {
      setTurns((prev) => [...prev, { question: q, answer: `Error: ${e.message}`, sources: [] }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Ask your finances</h1>
          <p>e.g. "How much did I spend on food last month compared to the month before?"</p>
        </div>
      </div>

      <div className="card">
        {turns.length === 0 && (
          <div className="empty-state">
            <IconChat />
            <div className="empty-state-title">Ask a question about your money</div>
            <p>Answers are grounded in your own transaction history.</p>
          </div>
        )}

        <div className="chat-scroll">
          {turns.map((t, i) => (
            <div key={i}>
              <div className="chat-row user"><div className="chat-bubble">{t.question}</div></div>
              <div className="chat-row assistant" style={{ marginTop: 8 }}>
                <div className="chat-bubble">
                  {t.answer}
                  {t.sources.length > 0 && (
                    <details className="chat-sources">
                      <summary>Sources used ({t.sources.length})</summary>
                      <ul>{t.sources.map((s, j) => <li key={j}>{s}</li>)}</ul>
                    </details>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="chat-input-row">
          <input
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && ask()}
            placeholder="Ask a question about your money..."
            disabled={loading}
          />
          <button onClick={ask} disabled={loading}>{loading ? "Thinking…" : "Ask"}</button>
        </div>
      </div>
    </div>
  );
}
