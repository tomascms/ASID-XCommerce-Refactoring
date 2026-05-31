import { check } from 'k6';
import { getAuthToken } from '../common/functions.js';

export const options = {
    vus: 25,
    duration: '10s',
};

export default function () {
    const authToken = getAuthToken();

    check(authToken, {
        'Authentication Token is not null': (authToken) => authToken !== null,
        'Authentication Token is a string': (authToken) => typeof authToken === 'string',
        'Authentication Token is not empty': (authToken) => authToken.length > 0,
    });

    if (authToken) {
        console.log(`Successfully obtained auth token: ${authToken.substring(0, 30)}...`);
    } else {
        console.error('Failed to obtain auth token.');
    }
}