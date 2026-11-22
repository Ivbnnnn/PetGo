import { useEffect, useState } from "react";
import { apiFetch as api } from "../api/client";

const roles = ["OWNER", "WALKER"];
const levels = ["VIEWER", "EDITOR", "ADMIN"];

export default function UsersPage() {
  const [users, setUsers] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ username: "", email: "", role: "OWNER", accessLevel: "VIEWER", enabled: true, temporaryPassword: "" });
  const [error, setError] = useState("");

  async function load() {
    try {
      setError("");
      const result = await api(`/api/admin/users?page=${page}&size=20&sort=email,asc`);
      setUsers(result.content ?? []);
      setTotal(result.totalElements ?? 0);
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => { load(); }, [page]);

  function openCreate() {
    setEditing("new");
    setForm({ username: "", email: "", role: "OWNER", accessLevel: "VIEWER", enabled: true, temporaryPassword: "" });
  }

  function openEdit(u) {
    setEditing(u);
    setForm({ username: u.username, email: u.email, role: u.role, accessLevel: u.accessLevel, enabled: u.enabled, temporaryPassword: "" });
  }

  async function save(e) {
    e.preventDefault();
    try {
      const body = editing === "new"
        ? { username: form.username, email: form.email, role: form.role, accessLevel: form.accessLevel, temporaryPassword: form.temporaryPassword }
        : { username: form.username, email: form.email, role: form.role, accessLevel: form.accessLevel, enabled: form.enabled, version: editing.version };
      await api(
        editing === "new" ? "/api/admin/users" : `/api/admin/users/${editing.id}`,
        { method: editing === "new" ? "POST" : "PUT", body: JSON.stringify(body) }
      );
      setEditing(null);
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  async function toggle(u) {
    try {
      await api(`/api/admin/users/${u.id}/${u.enabled ? "block" : "unblock"}`, { method: "POST" });
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  async function resetPassword(u) {
    const pwd = window.prompt("Временный пароль (от 12 символов)");
    if (!pwd) return;
    try {
      await api(`/api/admin/users/${u.id}/reset-password`, { method: "POST", body: JSON.stringify({ temporaryPassword: pwd }) });
      alert("Пароль сброшен");
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <main style={{ padding: 24, maxWidth: 1000, margin: "auto" }}>
      <h1>Пользователи</h1>
      {error && <p style={{ color: "crimson" }}>{error}</p>}
      <button onClick={openCreate}>Добавить пользователя</button>

      <table cellPadding="9" border="1" style={{ width: "100%", marginTop: 16 }}>
        <thead><tr><th>Имя</th><th>Email</th><th>Роль</th><th>Уровень</th><th>Статус</th><th>Действия</th></tr></thead>
        <tbody>
          {users.map(u => (
            <tr key={u.id}>
              <td>{u.username}</td>
              <td>{u.email}</td>
              <td>{u.role}</td>
              <td>{u.accessLevel}</td>
              <td>{u.enabled ? "Активен" : "Заблокирован"}</td>
              <td>
                <button onClick={() => openEdit(u)}>Изменить</button>
                <button onClick={() => toggle(u)}>{u.enabled ? "Блок" : "Разблок"}</button>
                <button onClick={() => resetPassword(u)}>Сбросить пароль</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div style={{ marginTop: 16 }}>
        <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>Назад</button>
        <span style={{ margin: "0 12px" }}>Стр. {page + 1}</span>
        <button disabled={(page + 1) * 20 >= total} onClick={() => setPage(p => p + 1)}>Вперёд</button>
      </div>

      {editing !== null && (
        <section style={{ border: "1px solid #aaa", padding: 20, marginTop: 24 }}>
          <h2>{editing === "new" ? "Новый пользователь" : "Редактирование"}</h2>
          <form onSubmit={save} style={{ display: "grid", gap: 12 }}>
            <input placeholder="Имя" value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} required />
            <input placeholder="Email" type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} required />
            <select value={form.role} onChange={e => setForm({ ...form, role: e.target.value })}>
              {roles.map(r => <option key={r}>{r}</option>)}
            </select>
            <select value={form.accessLevel} onChange={e => setForm({ ...form, accessLevel: e.target.value })}>
              {levels.map(l => <option key={l}>{l}</option>)}
            </select>
            {editing !== "new" && (
              <label><input type="checkbox" checked={form.enabled} onChange={e => setForm({ ...form, enabled: e.target.checked })} /> Активен</label>
            )}
            {editing === "new" && (
              <input placeholder="Временный пароль (от 12 символов)" value={form.temporaryPassword} onChange={e => setForm({ ...form, temporaryPassword: e.target.value })} required />
            )}
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