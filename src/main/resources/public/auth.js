import fetcher from "./fetcher";
import storage from "./storage";

/**
 * Verifies the user is logged in or redirects to login page
 * If the user is logged in, stores the user in storage.user
 * @returns {Promise<void>}
 */
export const verifyLogin = async () => {
    try {
        storage.user = await fetcher.verifyLoggedIn();
    } catch (e) {
        window.location.href = '/login.html';
    }
}