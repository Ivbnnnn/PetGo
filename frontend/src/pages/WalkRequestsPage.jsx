
import { useEffect, useState } from "react";

const emptyForm = {
  petId: "",
  ownerId: "",
  walkerId: "",
  scheduledAt: "",
  durationMinutes: 30,
  address: "",
  description: "",
  price: 0,
};

const statuses = ["CREATED", "IN_PROGRESS", "COMPLETED", "CANCELLED"];

export default function WalkRequestsPage({ api, accessLevel }) {
  const [requests, setRequests] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [sort, setSort] = useState("scheduledAt,desc");
  const [archived, setArchived] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [selected, setSelected] = useState(null);
  const [notes, setNotes] = useState([]);
  const [noteText, setNoteText] = useState("");
  const [history, setHistory] = useState(null);
  const [historyForm, setHistoryForm] = useState({
    actualDuration: 30,
    route: "",
    ownerReview: "",
    walkerReview: "",
    rating: 5,
  });

  const canEdit = accessLevel === "EDITOR" || accessLevel === "ADMIN";
  const isAdmin = accessLevel === "ADMIN";

  async function load() {
    try {
      setError("");
      const params = new URLSearchParams({
        page: String(page),
        size: "20",
        sort,
        isArchived: String(archived),
      });
      if (search.trim()) params.set("search", search.trim());
      if (status) params.set("status", status);

      const result = await api(`/api/walk-requests?${params}`);
      setRequests(result.content ?? []);
      setTotal(result.totalElements ?? 0);
    } catch (e) {
      setError(e.message || "Ошибка загрузки заявок");
    }
  }

  useEffect(() => {
    load();
  }, [page, status, sort, archived]);

  async function view(item) {
    try {
      setSelected(item);
      setHistory(null);
      setNotes(await api(`/api/walk-requests/${item.id}/notes`));
      try {
        const result = await api(
          `/api/walk-requests/${item.id}/history`
        );
        setHistory(result);
        setHistoryForm({
          actualDuration: result.actualDuration ?? item.durationMinutes,
          route: result.route ?? "",
          ownerReview: result.ownerReview ?? "",
          walkerReview: result.walkerReview ?? "",
          rating: result.rating ?? 5,
        });
      } catch {
        // История ещё не создана.
      }
    } catch (e) {
      setError(e.message);
    }
  }

  function openCreate() {
    setEditing("new");
    setForm(emptyForm);
    setFieldErrors({});
  }

  function openEdit(item) {
    setEditing(item);
    setFieldErrors({});
    setForm({
      petId: item.petId,
      ownerId: item.ownerId,
      walkerId: item.walkerId ?? "",
      scheduledAt: item.scheduledAt?.slice(0, 16) ?? "",
      durationMinutes: item.durationMinutes,
      address: item.address,
      description: item.description ?? "",
      price: item.price,
    });
  }

  function change(name, value) {
    setForm(current => ({ ...current, [name]: value }));
  }

  async function save(e) {
    e.preventDefault();
    setError("");
    setFieldErrors({});

    const creating = editing === "new";
    const body = creating
      ? {
          ...form,
          walkerId: form.walkerId || null,
          durationMinutes: Number(form.durationMinutes),
          price: Number(form.price),
          scheduledAt: form.scheduledAt + ":00",
        }
      : {
          version: editing.version,
          walkerId: form.walkerId || null,
          scheduledAt: form.scheduledAt + ":00",
          durationMinutes: Number(form.durationMinutes),
          address: form.address,
          description: form.description,
          price: Number(form.price),
        };

    try {
      await api(
        creating
          ? "/api/walk-requests"
          : `/api/walk-requests/${editing.id}`,
        {
          method: creating ? "POST" : "PUT",
          body: JSON.stringify(body),
        }
      );
      setEditing(null);
      await load();
    } catch (e) {
      if (e.status === 409) {
        setError("Заявка уже изменена. Обновите список и повторите действие.");
      } else {
        setError(e.message || "Ошибка сохранения");
      }
      setFieldErrors(e.fieldErrors ?? {});
    }
  }

  async function action(item, operation, method = "POST") {
    try {
      await api(`/api/walk-requests/${item.id}/${operation}`, { method });
      await load();
      setSelected(null);
    } catch (e) {
      setError(e.message || "Не удалось выполнить действие");
    }
  }

  async function archive(item) {
    try {
      await api(`/api/walk-requests/${item.id}`, { method: "DELETE" });
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  async function permanentlyDelete(item) {
    if (!window.confirm("Удалить заявку окончательно?")) return;
    try {
      await api(`/api/walk-requests/${item.id}/permanent`, {
        method: "DELETE",
      });
      await load();
      setSelected(null);
    } catch (e) {
      setError(e.message);
    }
  }

  async function addNote() {
    if (!selected || !noteText.trim()) return;
    try {
      await api(`/api/walk-requests/${selected.id}/notes`, {
        method: "POST",
        body: JSON.stringify({ content: noteText }),
      });
      setNoteText("");
      setNotes(await api(`/api/walk-requests/${selected.id}/notes`));
    } catch (e) {
      setError(e.message);
    }
  }

  async function noteAction(note, operation) {
    try {
      await api(`/api/notes/${note.id}${operation}`, {
        method: operation ? "POST" : "DELETE",
      });
      setNotes(await api(`/api/walk-requests/${selected.id}/notes`));
    } catch (e) {
      setError(e.message);
    }
  }

  async function saveHistory() {
    try {
      const updated = await api(
        `/api/walk-requests/${selected.id}/history`,
        {
          method: "POST",
          body: JSON.stringify({
            ...historyForm,
            actualDuration: Number(historyForm.actualDuration),
            rating: Number(historyForm.rating),
          }),
        }
      );
      setHistory(updated);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }

  const field = (name, label, type = "text") => (
    <label style={{ display: "grid", gap: 4 }}>
      {label}
      <input
        type={type}
        value={form[name] ?? ""}
        onChange={e => change(name, e.target.value)}
        required={["petId", "ownerId", "scheduledAt", "address"].includes(name)}
      />
      {fieldErrors[name] && (
        <small style={{ color: "crimson" }}>{fieldErrors[name]}</small>
      )}
    </label>
  );

  return (
    <main style={{ padding: 24, maxWidth: 1200, margin: "auto" }}>
      <h1>Заявки на выгул</h1>

      {error && (
        <p role="alert" style={{ color: "crimson" }}>{error}</p>
      )}

      <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
        <input
          placeholder="Поиск по питомцу, владельцу, адресу"
          value={search}
          onChange={e => setSearch(e.target.value)}
          onKeyDown={e => e.key === "Enter" && (setPage(0), load())}
        />
        <button onClick={() => { setPage(0); load(); }}>Найти</button>

        <select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}>
          <option value="">Все статусы</option>
          {statuses.map(s => <option key={s}>{s}</option>)}
        </select>

        <select value={sort} onChange={e => setSort(e.target.value)}>
          <option value="scheduledAt,desc">Дата ↓</option>
          <option value="scheduledAt,asc">Дата ↑</option>
          <option value="price,desc">Цена ↓</option>
          <option value="price,asc">Цена ↑</option>
          <option value="durationMinutes,desc">Длительность ↓</option>
        </select>

        <label>
          <input
            type="checkbox"
            checked={archived}
            onChange={e => { setArchived(e.target.checked); setPage(0); }}
          />
          Архив
        </label>

        {canEdit && <button onClick={openCreate}>Добавить заявку</button>}
      </div>

      <p>Всего заявок: {total}</p>

      <div style={{ overflowX: "auto" }}>
        <table cellPadding="9" border="1" style={{ width: "100%" }}>
          <thead>
            <tr>
              <th>Код</th>
              <th>Дата</th>
              <th>Длительность</th>
              <th>Цена</th>
              <th>Статус</th>
              <th>Приоритет</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {requests.map(item => (
              <tr key={item.id}>
                <td>{item.externalId}</td>
                <td>{item.scheduledAt?.replace("T", " ")}</td>
                <td>{item.durationMinutes} мин.</td>
                <td>{item.price} ₽</td>
                <td>{item.status}</td>
                <td>{item.priority}</td>
                <td>
                  <button onClick={() => view(item)}>Открыть</button>
                  {canEdit && !item.isArchived && (
                    <button onClick={() => openEdit(item)}>Изменить</button>
                  )}
                  {canEdit && !item.isArchived && (
                    <button onClick={() => archive(item)}>Архив</button>
                  )}
                  {canEdit && item.isArchived && (
                    <button onClick={() => action(item, "restore")}>Восстановить</button>
                  )}
                  {isAdmin && item.isArchived && (
                    <button onClick={() => permanentlyDelete(item)}>
                      Удалить
                    </button>
                  )}
                  {canEdit && !item.isArchived && item.status === "CREATED" && (
                    <button onClick={() => action(item, "start")}>Начать</button>
                  )}
                  {canEdit && !item.isArchived && item.status === "IN_PROGRESS" && (
                    <button onClick={() => action(item, "complete")}>Завершить</button>
                  )}
                  {canEdit && !item.isArchived &&
                    ["CREATED", "IN_PROGRESS"].includes(item.status) && (
                      <button onClick={() => action(item, "cancel")}>Отменить</button>
                    )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 16 }}>
        <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>
          Назад
        </button>
        <span>Страница {page + 1}</span>
        <button disabled={(page + 1) * 20 >= total} onClick={() => setPage(p => p + 1)}>
          Вперёд
        </button>
      </div>

      {editing !== null && (
        <section style={{ border: "1px solid #aaa", padding: 20, marginTop: 24 }}>
          <h2>{editing === "new" ? "Создание заявки" : "Редактирование заявки"}</h2>
          <form onSubmit={save} style={{ display: "grid", gap: 12 }}>
            {field("ownerId", "UUID владельца")}
            {field("petId", "UUID питомца")}
            {field("walkerId", "UUID выгульщика")}
            {field("scheduledAt", "Дата и время", "datetime-local")}
            {field("durationMinutes", "Длительность", "number")}
            {field("price", "Цена", "number")}
            {field("address", "Адрес")}
            {field("description", "Описание")}
            <div>
              <button type="submit">Сохранить</button>
              <button type="button" onClick={() => setEditing(null)}>
                Отмена
              </button>
            </div>
          </form>
        </section>
      )}

      {selected && (
        <section style={{ marginTop: 24 }}>
          <h2>Заявка {selected.externalId}</h2>
          <p>Адрес: {selected.address}</p>
          <p>Описание: {selected.description || "—"}</p>

          <h3>Заметки</h3>
          {notes.map(note => (
            <div key={note.id}>
              {note.pinned && "📌 "}
              {note.content}
              {canEdit && (
                <>
                  <button onClick={() => noteAction(note, note.pinned ? "/unpin" : "/pin")}>
                    {note.pinned ? "Открепить" : "Закрепить"}
                  </button>
                  <button onClick={() => noteAction(note, "")}>Удалить</button>
                </>
              )}
            </div>
          ))}
          {canEdit && (
            <div>
              <input
                value={noteText}
                onChange={e => setNoteText(e.target.value)}
                placeholder="Новая заметка"
              />
              <button onClick={addNote}>Добавить заметку</button>
            </div>
          )}

          <h3>История прогулки</h3>
          {selected.status !== "COMPLETED" ? (
            <p>История появится после завершения прогулки.</p>
          ) : (
            <div style={{ display: "grid", gap: 8, maxWidth: 500 }}>
              <input type="number" min="1"
                value={historyForm.actualDuration}
                onChange={e => setHistoryForm(f => ({ ...f, actualDuration: e.target.value }))}
              />
              <input placeholder="Маршрут"
                value={historyForm.route}
                onChange={e => setHistoryForm(f => ({ ...f, route: e.target.value }))}
              />
              <textarea placeholder="Отзыв владельца"
                value={historyForm.ownerReview}
                onChange={e => setHistoryForm(f => ({ ...f, ownerReview: e.target.value }))}
              />
              <textarea placeholder="Отзыв выгульщика"
                value={historyForm.walkerReview}
                onChange={e => setHistoryForm(f => ({ ...f, walkerReview: e.target.value }))}
              />
              <select value={historyForm.rating}
                onChange={e => setHistoryForm(f => ({ ...f, rating: e.target.value }))}>
                {[1, 2, 3, 4, 5].map(n => <option key={n} value={n}>{n}</option>)}
              </select>
              {canEdit && (
                <button onClick={saveHistory}>
                  {history ? "Изменить историю" : "Сохранить историю"}
                </button>
              )}
            </div>
          )}
          <button onClick={() => setSelected(null)}>Закрыть карточку</button>
        </section>
      )}
    </main>
  );
}
