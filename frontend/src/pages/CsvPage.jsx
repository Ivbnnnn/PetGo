import { useState } from "react";
import { api, apiFetch } from "../api/client";

export default function CsvPage() {
  const [file, setFile] = useState(null);
  const [mode, setMode] = useState("ONLY_NEW");
  const [report, setReport] = useState(null);
  const [error, setError] = useState("");

  async function preview() {
    if (!file) return;
    const fd = new FormData();
    fd.append("file", file);
    try {
      const data = await apiFetch(`/walk-requests/import/preview?mode=${mode}`, {
        method: "POST",
        body: fd, 
      });
      setReport(data);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }

  async function apply() {
    if (!file) return;
    const fd = new FormData();
    fd.append("file", file);
    try {
      const data = await apiFetch(`/walk-requests/import/apply?mode=${mode}`, {
        method: "POST",
        body: fd,
      });
      setReport(data);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }

  async function exportCsv() {
    try {
      const response = await api.get("/walk-requests/export", { responseType: "blob" });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const a = document.createElement("a");
      a.href = url;
      a.download = `walk-requests-${new Date().toISOString().slice(0, 10)}.csv`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      setError(e.response?.data?.message || e.message);
    }
  }

  return (
    <main style={{ padding: 24, maxWidth: 800, margin: "auto" }}>
      <h1>CSV: Экспорт / Импорт</h1>
      {error && <p style={{ color: "crimson" }}>{error}</p>}

      <section style={{ border: "1px solid #aaa", padding: 20, marginBottom: 24 }}>
        <h2>Экспорт</h2>
        <button onClick={exportCsv}>Скачать CSV</button>
      </section>

      <section style={{ border: "1px solid #aaa", padding: 20 }}>
        <h2>Импорт</h2>
        <input type="file" accept=".csv" onChange={e => setFile(e.target.files[0])} />
        <select value={mode} onChange={e => setMode(e.target.value)}>
          <option value="ONLY_NEW">Только новые</option>
          <option value="UPDATE_BY_EXTERNAL_ID">Обновить по external_id</option>
        </select>
        <div style={{ marginTop: 10 }}>
          <button onClick={preview}>Предпросмотр</button>
          <button onClick={apply} disabled={!report}>Применить</button>
        </div>

        {report && (
          <div style={{ marginTop: 16 }}>
            <p>Всего строк: {report.totalRows}</p>
            <p>Валидных: {report.validRows}</p>
            <p>Ошибок: {report.errorRows}</p>
            <p>Создать: {report.toCreate}, Обновить: {report.toUpdate}, Пропустить: {report.skipped}</p>
            {report.errors?.length > 0 && (
              <details>
                <summary>Ошибки по строкам</summary>
                <ul>{report.errors.map(e => <li key={e.line}>Строка {e.line}: {e.message}</li>)}</ul>
              </details>
            )}
          </div>
        )}
      </section>
    </main>
  );
}