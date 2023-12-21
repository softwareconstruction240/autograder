/**
 * @typedef {Object} User
 * @property {string} netId - The netId of the user
 * @property {string} firstName - The first name of the user
 * @property {string} lastName - The last name of the user
 * @property {string} repoUrl - The repository URL of the user
 * @property {string} role - The role of the user
 */

/**
 * @typedef {Object} TestResult
 * @property {string} testName - The name of the test
 * @property {boolean} passed - Whether the test passed or not
 * @property {string} errorMessage - The error message of the test
 * @property {TestResult} children - The children of the test
 * @property {number} numTestsPassed - The number of tests passed
 * @property {number} numTestsFailed - The number of tests failed
 */

/**
 * @typedef {Object} Submission
 * @property {string} netId - The netId of the user
 * @property {string} repoUrl - The repository URL of the user
 * @property {string} headHash - The hash of the head commit of the submission
 * @property {string} timestamp - The timestamp of the submission
 * @property {number} phase - The phase of the submission
 * @property {number} score - The score of the submission
 * @property {TestResult} testResults - The test results of the submission
 */

/**
 * @typedef {0 | 1 | 3 | 4 | 6} Phase
 */