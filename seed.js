const admin = require('firebase-admin');

// Connect to local emulator
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8088';
process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';

admin.initializeApp({
  projectId: 'vula-6d723'
});

const db = admin.firestore();
const auth = admin.auth();

const dummyNames = [
  'Alice', 'Bob', 'Charlie', 'Diana', 'Eve', 
  'Frank', 'Grace', 'Hank', 'Ivy', 'Jack'
];

async function seed() {
  console.log('Seeding data to local emulator...');

  for (let i = 0; i < dummyNames.length; i++) {
    const name = dummyNames[i];
    const username = name.toLowerCase();
    const email = `${username}@example.com`;
    const password = 'password123';

    try {
      // 1. Create User in Auth
      const userRecord = await auth.createUser({
        uid: `user_${i}`,
        email: email,
        password: password,
        displayName: name,
      });

      console.log(`Created user: ${username} (${userRecord.uid})`);

      // 2. Create User document in Firestore
      await db.collection('users').doc(userRecord.uid).set({
        id: userRecord.uid,
        username: username,
        createdAt: Date.now()
      });

      // 3. Create a Dummy Post for the user
      const postRef = db.collection('posts').doc();
      await postRef.set({
        id: postRef.id,
        authorId: userRecord.uid,
        authorUsername: username,
        caption: `Hello from ${name}! This is a dummy post to test the Vula Feed.`,
        createdAt: Date.now() - (i * 100000), // Stagger timestamps
        likesCount: 0,
        commentsCount: 0
      });

      console.log(`Created post for ${username}`);

    } catch (error) {
      console.error(`Error creating data for ${name}:`, error.message);
    }
  }

  console.log('Seeding complete!');
  process.exit(0);
}

seed();
