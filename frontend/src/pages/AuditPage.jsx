import { useEffect, useState } from "react";
import { apiFetch as api } from "../api/client";

const types = ["USER", "WALK_REQUEST", "PET", "DISTRICT", "TAG"];

export default function AuditPage() {
  const [logs, setLogs] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [q, setQ] = useState("");
  const [entityType, setEntityType] = useState("");
  const [error, setError] = useState("");

  async function load() {
    try {
      setError("");
      const params = new URLSearchParams({ page: String(page), size: "20", sort: "occurredAt,desc" });
      if (q.trim()) params.set("q", q.trim());
      if (entityType) params.set("entityType", entityType);
      const result = await api(`/api/admin/audit?${params}`);
      setLogs(result.content ?? []);
      setTotal(result.totalElements ?? 0);
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => { load(); }, [page, entityType]);

  return (
    <main style={{ padding: 24, maxWidth: 1100, margin: "auto" }}>
      <h1>Журнал аудита</h1>
      {error && <p style={{ color: "crimson" }}>{error}</p>}
      <div style={{ display: "flex", gap: 10 }}>
        <input placeholder="Поиск" value={q} onChange={e => setQ(e.target.value)} onKeyDown={e => e.key === "Enter" && (setPage(0), load())} />
        <button onClick={() => { setPage(0); load(); }}>Найти</button>
        <select value={entityType} onChange={e => { setEntityType(e.target.value); setPage(0); }}>
          <option value="">Все сущности</option>
          {types.map(t => <option key={t}>{t}</option>)}
        </select>
      </div>

      <table cellPadding="9" border="1" style={{ width: "100%", marginTop: 16 }}>
        <thead><tr><th>Время</th><th>Действие</th><th>Сущность</th><th>ID</th><th>Описание</th></tr></thead>
        <tbody>
          {logs.map(l => (
            <tr key={l.id}>
              <td>{l.occurredAt?.replace("T", " ").slice(0, 19)}</td>
              <td>{l.action}</td>
              <td>{l.entityType}</td>
              <td>{l.entityId?.slice(0, 8) || "—"}</td>
              <td>{l.description}</td>
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