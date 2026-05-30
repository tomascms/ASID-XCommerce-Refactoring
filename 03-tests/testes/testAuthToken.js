import { check } from 'k6';
import { getAuthToken } from '../common/functions.js'; // Adjust path if necessary

export const options = {
    vus: 50,
    duration: '10s',
};

export default function () {
    const authToken = getAuthToken();

    check(authToken, {
        'Authentication Token is not null': (token) => token !== null,
        'Authentication Token is a string': (token) => typeof token === 'string',
        'Authentication Token is not empty': (token) => token.length > 0,
    });

    if (authToken) {
        console.log(`Successfully obtained auth token: ${authToken.substring(0, 30)}...`);
    } else {
        console.error('Failed to obtain auth token.');
    }
}