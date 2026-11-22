import { useEffect, useState } from "react";
import { apiFetch as api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { isAdmin } from "../utils/roles";

export default function DistrictsPage() {
  const { user } = useAuth();
  const admin = isAdmin(user);
  const [districts, setDistricts] = useState([]);
  const [editing, setEditing] = useState(null);
  const [name, setName] = useState("");
  const [error, setError] = useState("");

  async function load() {
    try {
      setError("");
      setDistricts(await api("/api/districts"));
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => { load(); }, []);

  async function save(e) {
    e.preventDefault();
    try {
      await api(
        editing === "new" ? "/api/districts" : `/api/districts/${editing.id}`,
        { method: editing === "new" ? "POST" : "PUT", body: JSON.stringify({ name }) }
      );
      setEditing(null);
      setName("");
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  async function toggle(d) {
    try {
      await api(`/api/districts/${d.id}/${d.enabled ? "disable" : "enable"}`, { method: "POST" });
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <main style={{ padding: 24, maxWidth: 800, margin: "auto" }}>
      <h1>Районы</h1>
      {error && <p style={{ color: "crimson" }}>{error}</p>}
      {admin && <button onClick={() => { setEditing("new"); setName(""); }}>Добавить район</button>}

      <table cellPadding="9" border="1" style={{ width: "100%", marginTop: 16 }}>
        <thead><tr><th>Название</th><th>Статус</th>{admin && <th>Действия</th>}</tr></thead>
        <tbody>
          {districts.map(d => (
            <tr key={d.id}>
              <td>{d.name}</td>
              <td>{d.enabled ? "Включён" : "Отключён"}</td>
              {admin && <td>
                <button onClick={() => { setEditing(d); setName(d.name); }}>Изменить</button>
                <button onClick={() => toggle(d)}>{d.enabled ? "Отключить" : "Включить"}</button>
              </td>}
            </tr>
          ))}
        </tbody>
      </table>

      {editing !== null && (
        <section style={{ border: "1px solid #aaa", padding: 20, marginTop: 24 }}>
          <h2>{editing === "new" ? "Новый район" : "Редактирование"}</h2>
          <form onSubmit={save} style={{ display: "grid", gap: 12 }}>
            <input placeholder="Название" value={name} onChange={e => setName(e.target.value)} required />
            <div>
              <button type="submit">Сохранить</button>
              <button type="button" onClick={() => setEditing(null)}>Отмена</button>
            </div>
          </form>
        </section>
      )}
    </main>
  );
}
