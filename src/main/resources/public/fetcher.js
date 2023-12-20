const fetcher = {
    /**
     * @returns {Promise<User>}
     */
    verifyLoggedIn: async () => {
        const res = await fetch('/api/me');
        if (res.status !== 200)
            throw Error('Unauthorized');
        return await res.json();

    },
    logout: async () => {
        // post to /logout
        const res = await fetch('/logout', {method: 'POST'});
        if (res.status !== 200)
            throw Error('Logout failed: ' + res.body);
    }
};

export default fetcher;