import { useEffect, useState } from "react";
import { apiFetch as api } from "../api/client";

export default function WalkHistoryPage() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [minRating, setMinRating] = useState("");
  const [error, setError] = useState("");

  async function load() {
    try {
      setError("");
      const params = new URLSearchParams({ page: String(page), size: "20" });
      if (minRating) params.set("minRating", minRating);
      const result = await api(`/api/walk-history?${params}`);
      setItems(result.content ?? []);
      setTotal(result.totalElements ?? 0);
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => { load(); }, [page, minRating]);

  return (
    <main style={{ padding: 24, maxWidth: 1000, margin: "auto" }}>
      <h1>История прогулок</h1>
      {error && <p style={{ color: "crimson" }}>{error}</p>}

      <select value={minRating} onChange={e => { setMinRating(e.target.value); setPage(0); }}>
        <option value="">Любая оценка</option>
        {[1, 2, 3, 4, 5].map(n => <option key={n} value={n}>От {n}</option>)}
      </select>

      <table cellPadding="9" border="1" style={{ width: "100%", marginTop: 16 }}>
        <thead><tr><th>Заявка</th><th>Длительность</th><th>Оценка</th><th>Маршрут</th><th>Завершено</th></tr></thead>
        <tbody>
          {items.map(h => (
            <tr key={h.id}>
              <td>{h.requestId?.slice(0, 8)}</td>
              <td>{h.actualDuration} мин.</td>
              <td>{h.rating ?? "—"}</td>
              <td>{h.route || "—"}</td>
              <td>{h.completedAt?.replace("T", " ").slice(0, 16)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <div style={{ marginTop: 16 }}>
        <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>Назад</button>
        <span style={{ margin: "0 12px" }}>Стр. {page + 1}</span>
        <button disabled={(page + 1) * 20 >= total} onClick={() => setPage(p => p + 1)}>Вперёд</button>
      </div>
    </main>
  );
}