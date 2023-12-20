const fetcher = {
    /**
     * @returns {Promise<User>}
     */
    verifyLoggedIn: async () => {
        const res = await fetch('/api/me');
        switch (res.status) {
            case 401:
                throw Error('Not logged in');
            case 403:
                throw Error('Not registered');
        }
        return await res.json();

    },
    register: async(firstName, lastName, repoUrl) => {
        const urlParams = new URLSearchParams();
        urlParams.append('firstName', firstName);
        urlParams.append('lastName', lastName);
        urlParams.append('repoUrl', repoUrl);
        const res = await fetch('/auth/register', {
            method: 'POST',
            body: urlParams
        });

        if (!res.ok)
            throw Error('Registration failed: ' + res.body);
    },
    logout: async () => {
        // post to /logout
        const res = await fetch('/auth/logout', {method: 'POST'});
        if (res.status !== 200)
            throw Error('Logout failed: ' + res.body);
    }
};

export default fetcher;