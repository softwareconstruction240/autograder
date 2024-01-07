import fetcher from "./fetcher.js";
import storage from "./storage.js";

/**
 * Verifies the user is logged in or redirects to login page
 * If the user is logged in, stores the user in storage.user
 * @returns {Promise<void>}
 */
export const verifyLogin = async () => {
    try {
        storage.user = await fetcher.verifyLoggedIn();
    } catch (e) {
        switch (e.message) {
            case 'Not logged in':
                window.location.href = '/login.html';
                break;
            case 'Not registered':
                window.location.href = '/register.html';
                break;
        }
    }
}

export const register = async (firstName, lastName, repoUrl) => {
    try {
        await fetcher.register(firstName, lastName, repoUrl);
        window.location.href = '/';
    } catch (e) {
        console.error(e);
    }
}