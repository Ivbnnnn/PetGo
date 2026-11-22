
import { useEffect, useState } from "react";
import { apiFetch as api, fetchAllPages } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { isAdmin, isEditor, isOwner, isWalker } from "../utils/roles";

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

function localDateTimeValue(value) {
  if (!value) return "";
  if (/Z$|[+-]\d{2}:?\d{2}$/.test(value)) {
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) {
      const pad = n => String(n).padStart(2, "0");
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }
  }
  return value.slice(0, 16);
}

function currentLocalDateTime() {
  const now = new Date(Date.now() + 2 * 60 * 1000);
  const pad = n => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
}

export default function WalkRequestsPage() {
  const { user } = useAuth();
  const [requests, setRequests] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [sort, setSort] = useState("scheduledAt,desc");
  const [archived, setArchived] = useState(false);
  const [tagIdFilter, setTagIdFilter] = useState("");
  
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [busyAction, setBusyAction] = useState(null);
  
  const [selected, setSelected] = useState(null);
  const [notes, setNotes] = useState([]);
  const [noteText, setNoteText] = useState("");
  const [history, setHistory] = useState(null);
  const [historyForm, setHistoryForm] = useState({ actualDuration: 30, route: "", ownerReview: "", walkerReview: "", rating: 5 });

  const [owners, setOwners] = useState([]);
  const [pets, setPets] = useState([]);
  const [petsLoading, setPetsLoading] = useState(false);
  const [walkers, setWalkers] = useState([]);
  const [allTags, setAllTags] = useState([]);

  const [tagToAdd, setTagToAdd] = useState("");


  const [selectedIds, setSelectedIds] = useState([]);
  const [bulkAction, setBulkAction] = useState("ARCHIVE");
  const [bulkStatus, setBulkStatus] = useState("IN_PROGRESS");
  const [bulkTagId, setBulkTagId] = useState("");
  const [bulkLoading, setBulkLoading] = useState(false);

  const admin = isAdmin(user);
  const canEdit = isEditor(user);
  const canCreate = admin || isOwner(user);
  const canManageRequest = item => canEdit || (isOwner(user) && item.ownerId === user.id);
  const canRunWalk = item => Boolean(item.walkerId) && (admin || (isWalker(user) && item.walkerId === user.id));
  const creating = editing === "new";
  const dateChanged = creating || form.scheduledAt !== localDateTimeValue(editing?.scheduledAt);
  const petOwnerId = creating ? form.ownerId : "";

  async function load(overrides = {}) {
    try {
      setError("");
      const params = new URLSearchParams({
        page: String(overrides.page ?? page),
        size: "20",
        sort,
        isArchived: String(archived),
      });
      const searchValue = overrides.search ?? search;
      if (searchValue.trim()) params.set("search", searchValue.trim());
      if (status) params.set("status", status);
      if (tagIdFilter) params.set("tagId", tagIdFilter);

      const result = await api(`/walk-requests?${params}`);
      setRequests(result.content ?? []);
      setTotal(result.totalElements ?? 0);
      setSelectedIds([]); 
    } catch (e) {
      setError(e.message || "Ошибка загрузки заявок");
    }
  }

  useEffect(() => {
    load();
  }, [page, status, sort, archived, tagIdFilter, user.id]);

  useEffect(() => {
    api("/tags").then(setAllTags).catch(e => setError(e.message));
    if (admin) {
      fetchAllPages("/admin/users")
        .then(users => {
          setOwners(users.filter(u => u.enabled && isOwner(u)));
          setWalkers(users.filter(u => u.enabled && isWalker(u)));
        })
        .catch(e => setError(e.message));
    }
  }, [admin, user.id]);

  useEffect(() => {
    if (!petOwnerId) {
      setPets([]);
      setPetsLoading(false);
      return;
    }
    let active = true;
    setPets([]);
    setPetsLoading(true);
    fetchAllPages("/pets", { ownerId: petOwnerId, sort: "name,asc" })
      .then(items => { if (active) setPets(items); })
      .catch(e => { if (active) setError(e.message || "Не удалось загрузить питомцев"); })
      .finally(() => { if (active) setPetsLoading(false); });
    return () => { active = false; };
  }, [petOwnerId]);

  async function view(item) {
    try {
      setSelected(item);
      setTagToAdd(""); 
      setHistory(null);
      setHistoryForm({
        actualDuration: item.durationMinutes ?? 30,
        route: "", ownerReview: "", walkerReview: "", rating: 5,
      });
      setError("");
      setNotes(await api(`/walk-requests/${item.id}/notes`));

      if (item.status !== "COMPLETED") return;

      try {
        const result = await api(`/walk-requests/${item.id}/history`);
        setHistory(result);
        setHistoryForm({
          actualDuration: result.actualDuration ?? item.durationMinutes ?? 30,
          route: result.route ?? "",
          ownerReview: result.ownerReview ?? "",
          walkerReview: result.walkerReview ?? "",
          rating: result.rating ?? 5,
        });
      } catch (historyError) {
        if (historyError.status !== 404) {
          setError(historyError.message || "Не удалось загрузить историю прогулки");
        }
      }
    } catch (e) {
      setError(e.message || "Не удалось открыть заявку");
    }
  }

  function openCreate() {
    setError("");
    setEditing("new");
    setForm({ ...emptyForm, ownerId: admin ? "" : user.id });
    setFieldErrors({});
  }

  function openEdit(item) {
    setEditing(item);
    setFieldErrors({});
    setForm({
      petId: item.petId,
      ownerId: item.ownerId,
      walkerId: item.walkerId ?? "",
      scheduledAt: localDateTimeValue(item.scheduledAt),
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

    if (!form.scheduledAt) {
      setFieldErrors({ scheduledAt: "Укажите дату и время прогулки" });
      return;
    }
    if (dateChanged && form.scheduledAt < currentLocalDateTime()) {
      setFieldErrors({ scheduledAt: "Выберите дату и время в будущем" });
      setError("Дата прогулки не может быть в прошлом.");
      return;
    }
    const scheduledAt = form.scheduledAt.length === 16 ? `${form.scheduledAt}:00` : form.scheduledAt;
    const body = creating
      ? {
          ...form,
          ownerId: admin ? form.ownerId : user.id,
          walkerId: form.walkerId || null,
          durationMinutes: Number(form.durationMinutes),
          price: Number(form.price),
          scheduledAt,
        }
      : {
          version: editing.version,
          ...(admin && form.walkerId !== (editing.walkerId ?? "") && { walkerId: form.walkerId }),
          ...(dateChanged && { scheduledAt }),
          durationMinutes: Number(form.durationMinutes),
          address: form.address,
          description: form.description,
          price: Number(form.price),
        };

    try {
      setSaving(true);
      await api(
        creating ? "/walk-requests" : `/walk-requests/${editing.id}`,
        { method: creating ? "POST" : "PUT", body: JSON.stringify(body) }
      );
      setEditing(null);
      await load();
    } catch (e) {
      setError(e.message || "Ошибка сохранения");
      setFieldErrors(e.fieldErrors ?? {});
    } finally {
      setSaving(false);
    }
  }

  async function action(item, operation) {
    try {
      setBusyAction(`${item.id}:${operation}`);
      setError("");
      await api(`/walk-requests/${item.id}/${operation}`, { method: "POST" });
      await load();
      setSelected(null);
    } catch (e) {
      setError(e.message || "Не удалось выполнить действие");
    } finally {
      setBusyAction(null);
    }
  }

  async function archive(item) {
    try {
      await api(`/walk-requests/${item.id}`, { method: "DELETE" });
      await load();
      setSelected(null);
    } catch (e) {
      setError(e.message);
    }
  }

  async function permanentlyDelete(item) {
    if (!window.confirm("Удалить заявку окончательно?")) return;
    try {
      await api(`/walk-requests/${item.id}/permanent`, { method: "DELETE" });
      await load();
      setSelected(null);
    } catch (e) {
      setError(e.message);
    }
  }

  async function addNote() {
    if (!selected || !noteText.trim()) return;
    try {
      await api(`/walk-requests/${selected.id}/notes`, {
        method: "POST",
        body: JSON.stringify({ content: noteText }),
      });
      setNoteText("");
      setNotes(await api(`/walk-requests/${selected.id}/notes`));
    } catch (e) {
      setError(e.message);
    }
  }

  async function noteAction(note, operation) {
    try {
      await api(`/notes/${note.id}${operation}`, {
        method: operation ? "POST" : "DELETE",
      });
      setNotes(await api(`/walk-requests/${selected.id}/notes`));
    } catch (e) {
      setError(e.message);
    }
  }

  async function saveHistory() {
    try {
      const updated = await api(`/walk-requests/${selected.id}/history`, {
        method: "POST",
        body: JSON.stringify({
          ...historyForm,
          actualDuration: Number(historyForm.actualDuration),
          rating: Number(historyForm.rating),
        }),
      });
      setHistory(updated);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }

  async function attachTag(tagId) {
    if (!tagId) return;
    try {
      await api(`/walk-requests/${selected.id}/tags/${tagId}`, { method: "POST" });
      const updatedRequest = await api(`/walk-requests/${selected.id}`);
      setSelected(updatedRequest); 
      setTagToAdd("");  
      await load(); 
    } catch (e) {
      setError(e.message);
    }
  }

  async function detachTag(tagId) {
    try {
      await api(`/walk-requests/${selected.id}/tags/${tagId}`, { method: "DELETE" });
      const updatedRequest = await api(`/walk-requests/${selected.id}`);
      setSelected(updatedRequest);
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  const handleSelectOne = (id) => {
    setSelectedIds(prev => 
      prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]
    );
  };

  async function executeBulk() {
    if (selectedIds.length === 0) return;
    if (!window.confirm(`Вы уверены, что хотите применить действие к ${selectedIds.length} заявкам?`)) return;

    const payload = { ids: selectedIds, action: bulkAction };

    if (bulkAction === "CHANGE_STATUS") payload.status = bulkStatus;
    if (bulkAction === "ADD_TAG" || bulkAction === "REMOVE_TAG") {
      if (!bulkTagId) {
        setError("Выберите тег для массовой операции");
        return;
      }
      payload.tagId = bulkTagId;
    }

    try {
      setBulkLoading(true);
      const result = await api("/walk-requests/bulk", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      
      alert(`Операция завершена. Успешно: ${result.succeeded}, Ошибок: ${result.failed}`);
      setSelectedIds([]);
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBulkLoading(false);
    }
  }

  const textField = (name, label, type = "text") => (
    <label style={{ display: "grid", gap: 4 }}>
      {label}
      <input
        type={type}
        min={name === "scheduledAt" && dateChanged ? currentLocalDateTime() : undefined}
        value={form[name] ?? ""}
        onChange={e => change(name, e.target.value)}
        required={["scheduledAt", "address"].includes(name)}
      />
      {fieldErrors[name] && <small style={{ color: "crimson" }}>{fieldErrors[name]}</small>}
    </label>
  );

  const selectField = (name, label, options, required = false, disabled = false) => (
    <label style={{ display: "grid", gap: 4 }}>
      {label}
      <select
        value={form[name] ?? ""}
        onChange={e => change(name, e.target.value)}
        required={required}
        disabled={disabled}
      >
        <option value="" disabled={required}>— Выберите —</option>
        {options.map(opt => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
      {fieldErrors[name] && <small style={{ color: "crimson" }}>{fieldErrors[name]}</small>}
    </label>
  );

  const availablePets = pets.filter(p => p.ownerId === petOwnerId);

  return (
    <main className="walk-page">
      <header className="walk-page__header">
        <div>
          <span className="walk-eyebrow">PETGO · УПРАВЛЕНИЕ</span>
          <h1>Заявки на выгул</h1>
          <p>Планируйте прогулки, назначайте выгульщиков и следите за статусами</p>
        </div>
      </header>

      {error && <div className="walk-alert" role="alert">{error}</div>}

      <div className="walk-toolbar">
        <input
          placeholder="Поиск по питомцу, владельцу, адресу"
          value={search}
          onChange={e => setSearch(e.target.value)}
          onKeyDown={e => e.key === "Enter" && (setPage(0), load({ page: 0 }))}
        />
        <button className="walk-btn walk-btn--primary" onClick={() => { setPage(0); load({ page: 0 }); }}>Найти</button>

        <select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}>
          <option value="">Все статусы</option>
          {statuses.map(s => <option key={s}>{s}</option>)}
        </select>

        <select value={tagIdFilter} onChange={e => { setTagIdFilter(e.target.value); setPage(0); }}>
          <option value="">Все теги</option>
          {allTags.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
        </select>

        <select value={sort} onChange={e => setSort(e.target.value)}>
          <option value="scheduledAt,desc">Дата ↓</option>
          <option value="scheduledAt,asc">Дата ↑</option>
          <option value="price,desc">Цена ↓</option>
          <option value="price,asc">Цена ↑</option>
        </select>

        <label>
          <input type="checkbox" checked={archived} onChange={e => { setArchived(e.target.checked); setPage(0); }} />
          Архив
        </label>

        {canCreate && (
          <button className="walk-btn" type="button" onClick={openCreate}>
            Добавить заявку
          </button>
        )}
      </div>

      {/* ПАНЕЛЬ МАССОВЫХ ОПЕРАЦИЙ */}
      {selectedIds.length > 0 && canEdit && (
        <div style={{ padding: 16, background: "#f0f9f4", border: "1px solid #b9d9c8", borderRadius: 12, marginBottom: 16, display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
          <span style={{ fontWeight: "bold" }}>Выбрано: {selectedIds.length}</span>
          
          <select value={bulkAction} onChange={e => setBulkAction(e.target.value)}>
            <option value="ARCHIVE">В архив</option>
            <option value="RESTORE">Из архива</option>
            <option value="CHANGE_STATUS">Сменить статус</option>
            <option value="ADD_TAG">Добавить тег</option>
            <option value="REMOVE_TAG">Удалить тег</option>
          </select>

          {bulkAction === "CHANGE_STATUS" && (
            <select value={bulkStatus} onChange={e => setBulkStatus(e.target.value)}>
              {statuses.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          )}

          {(bulkAction === "ADD_TAG" || bulkAction === "REMOVE_TAG") && (
            <select value={bulkTagId} onChange={e => setBulkTagId(e.target.value)}>
              <option value="">— Выберите тег —</option>
              {allTags.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          )}

          <button className="walk-btn walk-btn--primary" onClick={executeBulk} disabled={bulkLoading}>
            {bulkLoading ? "Выполняется..." : "Применить"}
          </button>
          <button className="walk-btn" onClick={() => setSelectedIds([])}>Сбросить</button>
        </div>
      )}

      <div className="walk-list-meta"><strong>{total}</strong> заявок найдено</div>

      <div className="walk-table-wrap">
        <table className="walk-table">
          <thead>
            <tr>
              {canEdit && <th style={{ width: 40 }}></th>}
              <th>Код</th>
              <th>Дата</th>
              <th>Цена</th>
              <th>Статус</th>
              <th>Приоритет</th>
              <th>Теги</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {requests.map(item => (
              <tr key={item.id}>
                {canEdit && (
                  <td>
                    <input 
                      type="checkbox" 
                      checked={selectedIds.includes(item.id)} 
                      onChange={() => handleSelectOne(item.id)} 
                    />
                  </td>
                )}
                <td>{item.externalId}</td>
                <td>{item.scheduledAt?.replace("T", " ")}</td>
                <td>{item.price} ₽</td>
                <td><span className={`walk-status walk-status--${item.status?.toLowerCase()}`}>{({CREATED:"Новая", IN_PROGRESS:"Идёт прогулка", COMPLETED:"Завершена", CANCELLED:"Отменена"})[item.status] || item.status}</span></td>
                <td>{item.priority}</td>
                <td>
                  <div style={{ display: "flex", gap: 4, flexWrap: "wrap" }}>
                    {item.tags?.map(t => (
                      <span key={t.id} style={{ background: t.color || "#26966f", color: "#fff", padding: "2px 6px", borderRadius: 4, fontSize: 11 }}>
                        {t.name}
                      </span>
                    ))}
                  </div>
                </td>
                <td>
                  <button className="walk-btn" onClick={() => view(item)}>Открыть</button>
                  {canManageRequest(item) && !item.isArchived && !["COMPLETED", "CANCELLED"].includes(item.status) && (
                    <button className="walk-btn" onClick={() => openEdit(item)}>Изменить</button>
                  )}
                  {canRunWalk(item) && !item.isArchived && item.status === "CREATED" && (
                    <button className="walk-btn walk-btn--primary" disabled={!!busyAction} onClick={() => action(item, "start")}>Начать</button>
                  )}
                  {canRunWalk(item) && !item.isArchived && item.status === "IN_PROGRESS" && (
                    <button className="walk-btn walk-btn--primary" disabled={!!busyAction} onClick={() => action(item, "complete")}>Завершить</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 16 }}>
        <button className="walk-btn" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Назад</button>
        <span>Страница {page + 1}</span>
        <button className="walk-btn" disabled={(page + 1) * 20 >= total} onClick={() => setPage(p => p + 1)}>Вперёд</button>
      </div>

      {editing !== null && (
        <section className="walk-form-card">
          <h2>{editing === "new" ? "Создание заявки" : "Редактирование заявки"}</h2>
          <form onSubmit={save} className="walk-form-grid">
            {admin && creating && (
              <label style={{ display: "grid", gap: 4 }}>
                Владелец
                <select
                  value={form.ownerId ?? ""}
                  onChange={e => setForm(f => ({ ...f, ownerId: e.target.value, petId: "" }))}
                  required
                >
                  <option value="">— Выберите —</option>
                  {owners.map(o => <option key={o.id} value={o.id}>{o.username} ({o.email})</option>)}
                </select>
                {fieldErrors.ownerId && <small style={{ color: "crimson" }}>{fieldErrors.ownerId}</small>}
              </label>
            )}

            {creating && selectField("petId", "Питомец",
              availablePets.map(p => ({ value: p.id, label: p.name })), true, !petOwnerId || petsLoading)}

            {admin && selectField("walkerId", "Выгульщик",
              walkers.map(w => ({ value: w.id, label: w.username })), Boolean(editing?.walkerId))}

            {textField("scheduledAt", "Дата и время", "datetime-local")}
            {textField("durationMinutes", "Длительность", "number")}
            {textField("price", "Цена", "number")}
            {textField("address", "Адрес")}
            {textField("description", "Описание")}

            <div>
              <button className="walk-btn walk-btn--primary" type="submit"
                disabled={saving || (creating && !availablePets.some(pet => pet.id === form.petId))}>
                {saving ? "Сохраняем…" : "Сохранить"}
              </button>
              <button className="walk-btn" type="button" disabled={saving} onClick={() => setEditing(null)}>Отмена</button>
            </div>
          </form>
        </section>
      )}

      {selected && (
        <section className="walk-details-card">
          <h2>Заявка {selected.externalId}</h2>
          <p>Адрес: {selected.address}</p>
          <p>Описание: {selected.description || "—"}</p>
          {canManageRequest(selected) && (
            <div>
              {selected.isArchived ? (
                <button className="walk-btn" disabled={!!busyAction} onClick={() => action(selected, "restore")}>Восстановить из архива</button>
              ) : (
                <>
                  {!["COMPLETED", "CANCELLED"].includes(selected.status) && (
                    <button className="walk-btn" disabled={!!busyAction} onClick={() => action(selected, "cancel")}>Отменить заявку</button>
                  )}
                  <button className="walk-btn" onClick={() => archive(selected)}>В архив</button>
                </>
              )}
            </div>
          )}

          <div style={{ margin: "16px 0" }}>
            <h3>Теги</h3>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 8 }}>
              {selected.tags?.map(t => (
                <span key={t.id} style={{ background: t.color || "#26966f", color: "#fff", padding: "4px 8px", borderRadius: 4, fontSize: 13, display: "inline-flex", alignItems: "center" }}>
                  {t.name}
                  {canEdit && (
                    <button 
                      className="walk-btn" 
                      style={{ marginLeft: 6, padding: "0 4px", minHeight: "auto", background: "transparent", color: "#fff", border: "none", cursor: "pointer" }}
                      onClick={() => detachTag(t.id)}
                    >×</button>
                  )}
                </span>
              ))}
              {(!selected.tags || selected.tags.length === 0) && <span style={{ color: "#888" }}>Нет тегов</span>}
            </div>
            
            {canEdit && (
              <select 
                value={tagToAdd}
                onChange={(e) => {
                  const newTagId = e.target.value;
                  setTagToAdd(newTagId);
                  attachTag(newTagId);
                }}
              >
                <option value="" disabled>Добавить тег...</option>
                {allTags
                  .filter(t => !selected.tags?.find(st => st.id === t.id))
                  .map(t => (
                    <option key={t.id} value={t.id}>{t.name}</option>
                  ))}
              </select>
            )}
          </div>

          <h3>Заметки</h3>
          {notes.map(note => (
            <div key={note.id} style={{ marginBottom: 8 }}>
              {note.pinned && "📌 "}
              {note.content}
              {canEdit && (admin || note.authorId === user.id) && (
                <span style={{ marginLeft: 8 }}>
                  <button className="walk-btn" onClick={() => noteAction(note, note.pinned ? "/unpin" : "/pin")}>
                    {note.pinned ? "Открепить" : "Закрепить"}
                  </button>
                  <button className="walk-btn walk-btn--danger" onClick={() => noteAction(note, "")}>Удалить</button>
                </span>
              )}
            </div>
          ))}
          {canEdit && (
            <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
              <input
                value={noteText}
                onChange={e => setNoteText(e.target.value)}
                placeholder="Новая заметка"
                style={{ flexGrow: 1 }}
              />
              <button className="walk-btn" type="button" onClick={addNote}>Добавить</button>
            </div>
          )}

          <h3>История прогулки</h3>
          {selected.status !== "COMPLETED" ? (
            <p>История появится после завершения прогулки.</p>
          ) : (
            <div style={{ display: "grid", gap: 8, maxWidth: 500 }}>
              <input type="number" min="1" placeholder="Факт. длительность" value={historyForm.actualDuration} onChange={e => setHistoryForm(f => ({ ...f, actualDuration: e.target.value }))} />
              <input placeholder="Маршрут" value={historyForm.route} onChange={e => setHistoryForm(f => ({ ...f, route: e.target.value }))} />
              <textarea placeholder="Отзыв владельца" value={historyForm.ownerReview} onChange={e => setHistoryForm(f => ({ ...f, ownerReview: e.target.value }))} />
              <textarea placeholder="Отзыв выгульщика" value={historyForm.walkerReview} onChange={e => setHistoryForm(f => ({ ...f, walkerReview: e.target.value }))} />
              <select value={historyForm.rating} onChange={e => setHistoryForm(f => ({ ...f, rating: e.target.value }))}>
                {[1, 2, 3, 4, 5].map(n => <option key={n} value={n}>{n} ★</option>)}
              </select>
              {canEdit && (
                <button className="walk-btn walk-btn--primary" type="button" onClick={saveHistory}>
                  {history ? "Изменить историю" : "Сохранить историю"}
                </button>
              )}
            </div>
          )}
          <button className="walk-btn" style={{ marginTop: 16 }} onClick={() => setSelected(null)}>Закрыть карточку</button>
        </section>
      )}
    </main>
  );
}
