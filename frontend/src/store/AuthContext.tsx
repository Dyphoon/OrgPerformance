import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { api } from '../api';
import type { User } from '../types';

interface AuthContextType {
  user: User | null;
  token: string | null;
  roles: string[];
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const [roles, setRoles] = useState<string[]>([]);

  useEffect(() => {
    if (token) {
      api.auth.getCurrentUser()
        .then(userData => {
          setUser(userData);
        })
        .catch(() => {
          logout();
        });
    }
  }, [token]);

  const login = async (username: string, password: string) => {
    const response = await api.auth.login(username, password);
    localStorage.setItem('token', response.token);
    setToken(response.token);
    setUser(response.user);
    setRoles(response.roles);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
    setRoles([]);
  };

  const hasRole = (role: string) => {
    return roles.includes(role);
  };

  return (
    <AuthContext.Provider value={{ user, token, roles, isAuthenticated: !!token, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
