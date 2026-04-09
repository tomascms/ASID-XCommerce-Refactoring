const bcrypt = require('bcrypt');

const password = '123456';
bcrypt.hash(password, 10, (err, hash) => {
  if (err) {
    console.error('Error:', err);
  } else {
    console.log('BCrypt hash for "123456":');
    console.log(hash);
    console.log('\nSQL UPDATE command:');
    console.log(`UPDATE users_micro SET password = '${hash}' WHERE username = 'admin';`);
  }
});
