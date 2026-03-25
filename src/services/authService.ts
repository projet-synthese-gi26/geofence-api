import { AuthResponse, LoginDTO, RegisterDTO, RegisterResponse } from "@/types/auth";

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

export const authService = {
    async register(registerData: RegisterDTO): Promise<RegisterResponse> {
        const response = await fetch(`${API_URL}/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(registerData),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Erreur lors de l\'inscription');
        }

        return response.json();
    },

    async login(loginData: LoginDTO): Promise<AuthResponse> {
        const response = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(loginData),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Erreur lors de la connexion');
        }

        return response.json();
    },

    async getCurrentUser(token: string): Promise<AuthResponse> {
        const response = await fetch(`${API_URL}/auth/verify-user`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
            },
        });

        if (!response.ok) {
            throw new Error('Session expirée ou invalide');
        }

        return response.json();
    },

    getToken(): string | null {
        if (typeof window !== 'undefined') {
            return localStorage.getItem('auth_token');
        }
        return null;
    },

    setToken(token: string): void {
        if (typeof window !== 'undefined') {
            localStorage.setItem('auth_token', token);
        }
    },

    removeToken(): void {
        if (typeof window !== 'undefined') {
            localStorage.removeItem('auth_token');
        }
    }
};

export default authService;