const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080/api";

function authHeaders(): Record<string, string> {
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
      ...(options.headers ?? {}),
    },
  });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`API error ${res.status}: ${body}`);
  }
  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text);
}

export interface Account {
  id: number;
  name: string;
  type: "CHECKING" | "SAVINGS" | "INVESTMENT" | "CASH" | "CREDIT_CARD";
  currency: string;
}

export interface Transaction {
  id: number;
  accountId: number;
  categoryId: number | null;
  amount: number;
  currency: string;
  description: string;
  merchant: string | null;
  occurredOn: string;
  source: "MANUAL" | "CSV_IMPORT";
  aiCategorized: boolean;
}

export interface CategorySummary {
  categoryName: string;
  total: number;
}

export interface Category {
  id: number;
  name: string;
  parentId: number | null;
  income: boolean;
}

export interface BudgetStatus {
  categoryId: number;
  limit: number;
  spent: number;
  overBudget: boolean;
}

export interface Holding {
  id: number;
  accountId: number;
  ticker: string;
  shares: number;
  costBasis: number;
  currentPrice: number | null;
  marketValue: number;
  gainLoss: number;
  gainLossPercent: number;
  priceIsEstimated: boolean;
  updatedAt: string;
}

export interface PortfolioSummary {
  totalCostBasis: number;
  totalMarketValue: number;
  totalGainLoss: number;
  totalGainLossPercent: number;
}

export const api = {
  login: (email: string, password: string) =>
    request<{ token: string; displayName: string }>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),

  register: (email: string, password: string, displayName: string) =>
    request<{ token: string; displayName: string }>("/auth/register", {
      method: "POST",
      body: JSON.stringify({ email, password, displayName }),
    }),

  listAccounts: () => request<Account[]>("/accounts"),

  createAccount: (payload: { name: string; type: Account["type"]; currency: string }) =>
    request<Account>("/accounts", { method: "POST", body: JSON.stringify(payload) }),

  listTransactions: () => request<Transaction[]>("/transactions"),

  createTransaction: (payload: Partial<Transaction>) =>
    request<Transaction>("/transactions", { method: "POST", body: JSON.stringify(payload) }),

  deleteTransaction: (id: number) => request<void>(`/transactions/${id}`, { method: "DELETE" }),

  spendByCategory: (from: string, to: string) =>
    request<CategorySummary[]>(`/analytics/spend-by-category?from=${from}&to=${to}`),

  netForRange: (from: string, to: string) =>
    request<number>(`/analytics/net?from=${from}&to=${to}`),

  listCategories: () => request<Category[]>("/categories"),

  createCategory: (payload: { name: string; income: boolean }) =>
    request<Category>("/categories", { method: "POST", body: JSON.stringify(payload) }),

  setBudget: (categoryId: number, month: string, limit: number) =>
    request<void>(`/budgets?categoryId=${categoryId}&month=${month}&limit=${limit}`, { method: "POST" }),

  budgetStatus: (month: string) => request<BudgetStatus[]>(`/budgets?month=${month}`),

  listHoldings: () => request<Holding[]>("/holdings"),

  holdingsSummary: () => request<PortfolioSummary>("/holdings/summary"),

  createHolding: (payload: { accountId: number; ticker: string; shares: number; costBasis: number }) =>
    request<Holding>("/holdings", { method: "POST", body: JSON.stringify(payload) }),

  updateHoldingPrice: (id: number, currentPrice: number) =>
    request<Holding>(`/holdings/${id}/price`, { method: "PUT", body: JSON.stringify({ currentPrice }) }),

  deleteHolding: (id: number) => request<void>(`/holdings/${id}`, { method: "DELETE" }),

  askFinanceChat: (question: string) =>
    request<{ answer: string; sourcesUsed: string[] }>("/chat/ask", {
      method: "POST",
      body: JSON.stringify({ question }),
    }),

  reindex: () => request<void>("/chat/reindex", { method: "POST" }),

  importCsv: async (accountId: number, file: File) => {
    const form = new FormData();
    form.append("accountId", String(accountId));
    form.append("file", file);
    const res = await fetch(`${API_BASE}/transactions/import?accountId=${accountId}`, {
      method: "POST",
      headers: authHeaders(),
      body: form,
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },
};
