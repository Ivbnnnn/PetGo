import { useEffect, useState } from "react";
import { apiFetch as api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { isEditor } from "../utils/roles";

export default function TagsPage() {
  const { user } = useAuth();
  const canEdit = isEditor(user);
  const [tags, setTags] = useState([]);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: "", color: "#26966f" });
  const [error, setError] = useState("");

  async function load() {
    try {
      setError("");
      setTags(await api("/api/tags"));
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => { load(); }, []);

  function openCreate() {
    setEditing("new");
    setForm({ name: "", color: "#26966f" });
  }

  function openEdit(tag) {
    setEditing(tag);
    setForm({ name: tag.name, color: tag.color || "#26966f" });
  }

  async function save(e) {
    e.preventDefault();
    try {
      await api(
        editing === "new" ? "/api/tags" : `/api/tags/${editing.id}`,
        { method: editing === "new" ? "POST" : "PUT", body: JSON.stringify(form) }
      );
      setEditing(null);
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  async function remove(tag) {
    if (!window.confirm("Удалить тег?")) return;
    try {
      await api(`/api/tags/${tag.id}`, { method: "DELETE" });
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <main style={{ padding: 24, maxWidth: 800, margin: "auto" }}>
      <h1>Теги</h1>
      {error && <p style={{ color: "crimson" }}>{error}</p>}
      {canEdit && <button onClick={openCreate}>Добавить тег</button>}

      <table cellPadding="9" border="1" style={{ width: "100%", marginTop: 16 }}>
        <thead><tr><th>Название</th><th>Цвет</th>{canEdit && <th>Действия</th>}</tr></thead>
        <tbody>
          {tags.map(t => (
            <tr key={t.id}>
              <td>{t.name}</td>
              <td>
                <span style={{ display: "inline-block", width: 16, height: 16, background: t.color, borderRadius: 4, marginRight: 6 }} />
                {t.color || "—"}
              </td>
              {canEdit && <td>
                <button onClick={() => openEdit(t)}>Изменить</button>
                <button onClick={() => remove(t)}>Удалить</button>
              </td>}
            </tr>
          ))}
        </tbody>
      </table>

      {editing !== null && (
        <section style={{ border: "1px solid #aaa", padding: 20, marginTop: 24 }}>
          <h2>{editing === "new" ? "Новый тег" : "Редактирование"}</h2>
          <form onSubmit={save} style={{ display: "grid", gap: 12 }}>
            <input placeholder="Название" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required />
            <input type="color" value={form.color} onChange={e => setForm({ ...form, color: e.target.value })} />
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
