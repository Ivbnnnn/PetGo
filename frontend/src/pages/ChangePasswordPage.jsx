import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, TextField, Button, Typography, Alert, Paper } from '@mui/material';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ChangePasswordPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [currentPassword, setCurrent] = useState('');
  const [newPassword, setNew] = useState('');
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await api.post('/auth/change-password', { currentPassword, newPassword });
      setDone(true);
      setTimeout(async () => {
        await logout();
        navigate('/login');
      }, 1500);
    } catch (err) {
      setError(err.response?.data?.message || 'Ошибка смены пароля');
    }
  };

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="#f5f5f5">
      <Paper sx={{ p: 4, width: 400 }}>
        <Typography variant="h5" mb={2}>Смена пароля</Typography>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {done ? (
          <Alert severity="success">Пароль изменён. Перенаправление на вход...</Alert>
        ) : (
          <form onSubmit={handleSubmit}>
            <TextField fullWidth label="Текущий пароль" type="password" value={currentPassword} onChange={e => setCurrent(e.target.value)} margin="normal" required />
            <TextField fullWidth label="Новый пароль" type="password" value={newPassword} onChange={e => setNew(e.target.value)} margin="normal" required helperText="От 12 символов до 72 байт" />
            <Button fullWidth type="submit" variant="contained" sx={{ mt: 2 }}>Сменить пароль</Button>
          </form>
        )}
      </Paper>
    </Box>
  );
}