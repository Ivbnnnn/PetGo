import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, TextField, Button, Typography, Alert, Paper } from '@mui/material';
import { useAuth } from '../auth/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const user = await login(email, password);
      navigate(user.passwordChangeRequired ? '/change-password' : '/');
    } catch (err) {
      setError(err.response?.data?.message || 'Ошибка входа');
    }
  };

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="#f5f5f5">
      <Paper sx={{ p: 4, width: 400 }}>
        <Typography variant="h5" mb={2} align="center">Вход в PetGo</Typography>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <form onSubmit={handleSubmit}>
          <TextField fullWidth label="Email" value={email} onChange={e => setEmail(e.target.value)} margin="normal" required />
          <TextField fullWidth label="Пароль" type="password" value={password} onChange={e => setPassword(e.target.value)} margin="normal" required />
          <Button fullWidth type="submit" variant="contained" sx={{ mt: 2 }}>Войти</Button>
        </form>
      </Paper>
    </Box>
  );
}