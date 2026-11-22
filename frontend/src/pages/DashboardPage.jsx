import { useEffect, useState } from "react";
import { apiFetch as api } from "../api/client";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell
} from "recharts";

const STATUS_COLORS = {
  CREATED: "#4c8fc7",
  IN_PROGRESS: "#d99a45",
  COMPLETED: "#26966f",
  CANCELLED: "#c96c66",
};

const PRIORITY_COLORS = {
  LOW: "#26966f",
  NORMAL: "#d99a45",
  HIGH: "#c96c66",
};

export default function DashboardPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");

  async function load() {
    try {
      setError("");
      const params = new URLSearchParams();
      if (from) params.append("from", from);
      if (to) params.append("to", to);

      const result = await api(`/api/analytics/summary?${params.toString()}`);
      setData(result);
    } catch (e) {
      setError(e.message || "Ошибка загрузки аналитики");
    }
  }

  useEffect(() => {
    load();
  }, []);

  if (error) return <p style={{ color: "crimson", padding: 24 }}>{error}</p>;
  if (!data) return <p style={{ padding: 24 }}>Загрузка...</p>;

  const statusData = Object.entries(data.byStatus || {}).map(([name, value]) => ({ name, value }));
  const priorityData = Object.entries(data.byPriority || {}).map(([name, value]) => ({ name, value }));

  return (
    <main style={{ padding: 24, maxWidth: 1100, margin: "auto", fontFamily: "sans-serif" }}>
      <h1>Аналитика</h1>

      <div style={{ display: "flex", gap: 12, marginBottom: 24 }}>
        <label>
          С даты:{" "}
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </label>
        <label>
          По дату:{" "}
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </label>
        <button onClick={load} style={{ padding: "4px 16px", cursor: "pointer" }}>
          Применить
        </button>
      </div>

      <section style={{ display: "flex", gap: 16, flexWrap: "wrap", marginBottom: 24 }}>
        <KpiCard label="Всего заявок" value={data.total} />
        <KpiCard label="Активных" value={data.active} />
        <KpiCard label="В архиве" value={data.archived} />
        <KpiCard label="В периоде" value={data.inPeriod} />
        <KpiCard label="Средняя цена" value={data.avgPrice?.toFixed(2)} />
        <KpiCard label="Выручка (заверш.)" value={data.revenueCompleted?.toFixed(2)} />
        <KpiCard label="Средний рейтинг" value={data.averageRating?.toFixed(2) || "—"} />
      </section>

      <div style={{ display: "flex", gap: 24, flexWrap: "wrap", marginBottom: 24 }}>
        <section style={{ flex: 1, minWidth: 300, border: "1px solid #ddd", borderRadius: 8, padding: 16 }}>
          <h3 style={{ marginTop: 0 }}>По статусам</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={statusData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="value">
                {statusData.map((entry, index) => (
                  <Cell key={index} fill={STATUS_COLORS[entry.name] || "#888"} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </section>

        <section style={{ flex: 1, minWidth: 300, border: "1px solid #ddd", borderRadius: 8, padding: 16 }}>
          <h3 style={{ marginTop: 0 }}>По приоритетам</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={priorityData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="value">
                {priorityData.map((entry, index) => (
                  <Cell key={index} fill={PRIORITY_COLORS[entry.name] || "#888"} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </section>
      </div>

      <div style={{ display: "flex", gap: 24, flexWrap: "wrap" }}>
        <TopList title="Топ питомцев" rows={data.topPets} />
        <TopList title="Топ владельцев" rows={data.topOwners} />
        <TopList title="Топ выгульщиков" rows={data.topWalkers} />
        <TopList title="По районам" rows={data.byDistrict} />
      </div>
    </main>
  );
}

function KpiCard({ label, value }) {
  return (
    <div style={{ border: "1px solid #ddd", borderRadius: 8, padding: 16, minWidth: 150 }}>
      <div style={{ fontSize: 13, color: "#666" }}>{label}</div>
      <div style={{ fontSize: 24, fontWeight: "bold" }}>{value ?? "—"}</div>
    </div>
  );
}

function TopList({ title, rows }) {
  return (
    <section style={{ flex: 1, minWidth: 240, border: "1px solid #ddd", borderRadius: 8, padding: 16 }}>
      <h3 style={{ marginTop: 0 }}>{title}</h3>
      {rows?.length > 0 ? (
        <ul style={{ paddingLeft: 16, margin: 0 }}>
          {rows.map((r) => (
            <li key={r.name} style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
              <span>{r.name}</span>
              <strong>{r.count}</strong>
            </li>
          ))}
        </ul>
      ) : (
        <p style={{ color: "#888" }}>Нет данных</p>
      )}
    </section>
  );
}