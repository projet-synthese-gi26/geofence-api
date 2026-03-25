import { UserRole } from './enums';

export interface UserDTO {
    uuid: string;
    username: string;
    firstname: string;
    lastname: string;
    phoneNumber: string;
    email: string;
    DOB: string;
    Role: UserRole;
}

export interface AuthResponse {
    userDTO: UserDTO;
    token: string;
}

export interface RegisterResponse {
    success: boolean;
    message: string;
    user: UserDTO;
}

export interface LoginUsernameDTO {
    type: 'username';
    username: string;
    password: string;
}

export interface LoginEmailDTO {
    type: 'email';
    email: string;
    password: string;
}

export interface LoginPhoneNumberDTO {
    type: 'phone';
    phoneNumber: string;
    password: string;
}

export type LoginDTO = LoginUsernameDTO | LoginEmailDTO | LoginPhoneNumberDTO;

export interface RegisterDTO {
    firstname: string;
    lastname: string;
    username: string;
    phoneNumber: string;
    email: string;
    password: string;
    password_confirmation: string;
    DOB: string;
}

export interface ErrorEntity {
    status: number;
    message: string;
}