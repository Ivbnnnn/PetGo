import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './petgo.css';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';

const theme = createTheme({
  palette: { mode: 'light', primary: { main: '#26966f', dark: '#187653', light: '#e6f5ee', contrastText: '#ffffff' }, secondary: { main: '#e9a45b' }, background: { default: '#f5f8f6', paper: '#ffffff' }, text: { primary: '#20332b', secondary: '#718078' }, success: { main: '#26966f' } },
  shape: { borderRadius: 14 },
  typography: { fontFamily: 'Inter, system-ui, -apple-system, BlinkMacSystemFont, sans-serif', h4: { fontWeight: 750, letterSpacing: '-0.035em' }, h5: { fontWeight: 700, letterSpacing: '-0.025em' }, h6: { fontWeight: 700 }, button: { textTransform: 'none', fontWeight: 650, borderRadius: 10 } },
  components: {
    MuiButton: { styleOverrides: { root: { boxShadow: 'none', paddingInline: 16 }, contained: { '&:hover': { boxShadow: 'none' } } } },
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } },
    MuiAppBar: { styleOverrides: { root: { backgroundColor: '#ffffff', color: '#20332b', boxShadow: '0 2px 18px rgba(30,70,50,.06)' } } },
    MuiTableCell: { styleOverrides: { head: { color: '#718078', fontWeight: 700, background: '#f7faf8' }, root: { borderColor: '#edf1ee' } } },
  }
});

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <App />
    </ThemeProvider>
  </React.StrictMode>
);