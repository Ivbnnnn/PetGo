import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  AppBar, Toolbar, Typography, Drawer, List, ListItemButton, ListItemIcon, ListItemText,
  Box, IconButton, Avatar, Menu, MenuItem, Divider, Button
} from '@mui/material';
import {
  Menu as MenuIcon, Dashboard, Assignment, Pets, LocalOffer, Map,
  People, History, UploadFile, Logout
} from '@mui/icons-material';
import { useAuth } from '../auth/AuthContext';

const DRAWER_WIDTH = 252;
const GREEN = '#26966f';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);

  const items = [
  { label: 'Дашборд', icon: <Dashboard />, path: '/' },
  { label: 'Заявки', icon: <Assignment />, path: '/walk-requests' },
  { label: 'Питомцы', icon: <Pets />, path: '/pets' },
  { label: 'Теги', icon: <LocalOffer />, path: '/tags' },
  { label: 'История', icon: <History />, path: '/walk-history' },
  { label: 'CSV', icon: <UploadFile />, path: '/csv' },
  { label: 'Районы', icon: <Map />, path: '/districts' },
  { label: 'Пользователи', icon: <People />, path: '/users' },
  { label: 'Аудит', icon: <History />, path: '/audit' },
];

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const drawer = (
    <Box sx={{ minHeight: '100%', bgcolor: '#fff' }}>
      <Toolbar sx={{ px: 2.5, minHeight: '76px !important' }}><Box sx={{ width: 38, height: 38, borderRadius: 2.5, bgcolor: '#e6f5ee', color: GREEN, display: 'grid', placeItems: 'center', mr: 1.5, fontSize: 22 }}>🐾</Box><Box><Typography variant="h6" sx={{ color: '#20332b', lineHeight: 1.1 }}>PetGo</Typography><Typography variant="caption" color="text.secondary">Забота о питомцах</Typography></Box></Toolbar>
      <Divider />
      <List>
        {items.map((item) => (
          <ListItemButton
            key={item.path}
            selected={location.pathname === item.path}
            onClick={() => { navigate(item.path); setMobileOpen(false); }}
            sx={{ mx: 1.25, my: .35, minHeight: 46, borderRadius: 2.5, '&.Mui-selected': { bgcolor: '#e8f5ee', color: GREEN, '& .MuiListItemIcon-root': { color: GREEN } }, '&.Mui-selected:hover': { bgcolor: '#dff0e7' }, '&:hover': { bgcolor: '#f5f8f6' } }}
          >
            <ListItemIcon sx={{ minWidth: 38, color: '#84948b' }}>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  );

  return (
    <Box display="flex">
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1, borderBottom: '1px solid #edf1ee' }}>
        <Toolbar sx={{ minHeight: '68px !important' }}>
          <IconButton color="inherit" edge="start" onClick={() => setMobileOpen(!mobileOpen)} sx={{ mr: 2, display: { sm: 'none' } }}>
            <MenuIcon />
          </IconButton>
          <Box sx={{ flexGrow: 1 }}><Typography variant="subtitle1" fontWeight={750}>Панель управления</Typography><Typography variant="caption" color="text.secondary">Сервис прогулок PetGo</Typography></Box>
          <IconButton onClick={(e) => setAnchorEl(e.currentTarget)}>
            <Avatar sx={{ width: 36, height: 36, bgcolor: '#e6f5ee', color: GREEN, fontWeight: 700 }}>{user?.username?.[0]?.toUpperCase()}</Avatar>
          </IconButton>
          <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled>{user?.username} ({user?.accessLevel})</MenuItem>
            <Divider />
            <MenuItem onClick={handleLogout}><Logout fontSize="small" sx={{ mr: 1 }} />Выйти</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { sm: DRAWER_WIDTH }, flexShrink: { sm: 0 } }}>
        <Drawer variant="temporary" open={mobileOpen} onClose={() => setMobileOpen(false)}
          sx={{ display: { xs: 'block', sm: 'none' }, '& .MuiDrawer-paper': { width: DRAWER_WIDTH } }}>
          {drawer}
        </Drawer>
        <Drawer variant="permanent"
          sx={{ display: { xs: 'none', sm: 'block' }, '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' } }}>
          {drawer}
        </Drawer>
      </Box>

      <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, md: 3.5 }, mt: 8.5, minHeight: '100vh', bgcolor: '#f5f8f6', width: { sm: `calc(100% - ${DRAWER_WIDTH}px)` }, ml: { sm: `${DRAWER_WIDTH}px` } }}>
        <Outlet />
      </Box>
    </Box>
  );
}