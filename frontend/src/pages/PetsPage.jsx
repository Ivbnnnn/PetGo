import { useEffect, useState } from "react";
import { apiFetch as api, fetchAllPages } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { isAdmin, isOwner } from "../utils/roles";

export default function PetsPage() {
  const { user } = useAuth();
  const admin = isAdmin(user);
  const owner = isOwner(user);
  const [pets, setPets] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: "", breed: "", weight: "", age: "", specialNeeds: "", photoUrl: "" });
  const [owners, setOwners] = useState([]);
  const [ownerId, setOwnerId] = useState("");
  const [error, setError] = useState("");

  async function load() {
    try {
      setError("");
      const result = await api(`/api/pets?page=${page}&size=20&sort=name,asc`);
      setPets(result.content ?? []);
      setTotal(result.totalElements ?? 0);
    } catch (e) {
      setError(e.message || "Ошибка загрузки питомцев");
    }
  }

  useEffect(() => { load(); }, [page]);

  useEffect(() => {
    if (!admin) return;
    fetchAllPages("/admin/users")
      .then(users => setOwners(users.filter(account => account.enabled && isOwner(account))))
      .catch(e => setError(e.message));
  }, [admin]);

  function openCreate() {
    setEditing("new");
    setOwnerId("");
    setForm({ name: "", breed: "", weight: "", age: "", specialNeeds: "", photoUrl: "" });
  }

  function openEdit(pet) {
    setEditing(pet);
    setForm({
      name: pet.name, breed: pet.breed || "", weight: pet.weight || "",
      age: pet.age || "", specialNeeds: pet.specialNeeds || "", photoUrl: pet.photoUrl || ""
    });
  }

  async function save(e) {
    e.preventDefault();
    try {
      const body = {
        ...form,
        ...(admin && editing === "new" && { ownerId }),
        weight: form.weight ? Number(form.weight) : null,
        age: form.age ? Number(form.age) : null,
      };
      await api(
        editing === "new" ? "/api/pets" : `/api/pets/${editing.id}`,
        { method: editing === "new" ? "POST" : "PUT", body: JSON.stringify(body) }
      );
      setEditing(null);
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  async function remove(pet) {
    if (!window.confirm("Удалить питомца?")) return;
    try {
      await api(`/api/pets/${pet.id}`, { method: "DELETE" });
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <main style={{ padding: 24, maxWidth: 1000, margin: "auto" }}>
      <h1>Питомцы</h1>
      {error && <p style={{ color: "crimson" }}>{error}</p>}
      {(admin || owner) && <button onClick={openCreate}>Добавить питомца</button>}

      <table cellPadding="9" border="1" style={{ width: "100%", marginTop: 16 }}>
        <thead>
          <tr><th>Имя</th><th>Порода</th><th>Вес</th><th>Возраст</th><th>Действия</th></tr>
        </thead>
        <tbody>
          {pets.map(p => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td>{p.breed || "—"}</td>
              <td>{p.weight || "—"}</td>
              <td>{p.age || "—"}</td>
              <td>
                {(admin || (owner && p.ownerId === user.id)) && (
                  <>
                    <button onClick={() => openEdit(p)}>Изменить</button>
                    <button onClick={() => remove(p)}>Удалить</button>
                  </>
                )}
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
          <h2>{editing === "new" ? "Новый питомец" : "Редактирование"}</h2>
          <form onSubmit={save} style={{ display: "grid", gap: 12 }}>
            {admin && editing === "new" && (
              <label>
                Владелец
                <select value={ownerId} onChange={e => setOwnerId(e.target.value)} required>
                  <option value="">Выберите владельца</option>
                  {owners.map(account => <option key={account.id} value={account.id}>{account.username} ({account.email})</option>)}
                </select>
              </label>
            )}
            <input placeholder="Имя" value={form.name} onChange={e => setForm({...form, name: e.target.value})} required />
            <input placeholder="Порода" value={form.breed} onChange={e => setForm({...form, breed: e.target.value})} />
            <input type="number" step="0.01" min="0.01" placeholder="Вес (кг)" value={form.weight} onChange={e => setForm({...form, weight: e.target.value})} />
            <input type="number" placeholder="Возраст" value={form.age} onChange={e => setForm({...form, age: e.target.value})} />
            <textarea placeholder="Особенности" value={form.specialNeeds} onChange={e => setForm({...form, specialNeeds: e.target.value})} />
            <input placeholder="Ссылка на фото" value={form.photoUrl} onChange={e => setForm({...form, photoUrl: e.target.value})} />
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
