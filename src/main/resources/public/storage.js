// basic storage for the app

/**
 * @typedef {Object} Storage
 * @property {User} user
 */
const storage = {
    user: {
        netId: null,
        firstName: null,
        lastName: null,
        repoUrl: null,
    }
};

export default storage;