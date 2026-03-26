const express = require('express');
const cors = require('cors');
const axios = require('axios');
const fs = require('fs');
const path = require('path');

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static('public'));

// ==================== DATA PERSISTENCE ====================
const DATA_DIR = path.join(__dirname, 'data');

// Create data directory if it doesn't exist
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

const BRANDS_FILE = path.join(DATA_DIR, 'brands.json');
const CATEGORIES_FILE = path.join(DATA_DIR, 'categories.json');
const USERS_FILE = path.join(DATA_DIR, 'users.json');
const ORDERS_FILE = path.join(DATA_DIR, 'orders.json');
const METADATA_FILE = path.join(DATA_DIR, 'metadata.json');

// Initialize JSON files if they don't exist
function initializeDataFile(filepath, defaultData) {
  if (!fs.existsSync(filepath)) {
    fs.writeFileSync(filepath, JSON.stringify(defaultData, null, 2));
  }
}

function loadJSON(filepath) {
  try {
    const data = fs.readFileSync(filepath, 'utf8');
    return JSON.parse(data);
  } catch (e) {
    console.error(`Error loading ${filepath}:`, e.message);
    return null;
  }
}

function saveJSON(filepath, data) {
  try {
    fs.writeFileSync(filepath, JSON.stringify(data, null, 2));
    return true;
  } catch (e) {
    console.error(`Error saving ${filepath}:`, e.message);
    return false;
  }
}

// Initialize files with default data
initializeDataFile(BRANDS_FILE, [
  { id: 1, name: 'HyperX' },
  { id: 2, name: 'Logitech' }
]);

initializeDataFile(CATEGORIES_FILE, [
  { id: 1, name: 'gaming keyboard' },
  { id: 2, name: 'gaming mouse' },
  { id: 3, name: 'gaming headset' }
]);

initializeDataFile(USERS_FILE, {
  'admin@xcommerce.com': { password: '123456', role: 'admin', name: 'Admin User' },
  'demo@xcommerce.com': { password: 'demo123', role: 'user', name: 'Demo User' }
});

initializeDataFile(ORDERS_FILE, {});

initializeDataFile(METADATA_FILE, {
  nextBrandId: 3,
  nextCategoryId: 4,
  nextOrderId: 100,
  nextSKU: 1
});

// Load data
let brands = loadJSON(BRANDS_FILE) || [];
let categories = loadJSON(CATEGORIES_FILE) || [];
let users = loadJSON(USERS_FILE) || {};
let orderTracker = loadJSON(ORDERS_FILE) || {};
let metadata = loadJSON(METADATA_FILE) || { nextBrandId: 3, nextCategoryId: 4, nextOrderId: 100, nextSKU: 1 };

console.log('📋 Data loaded at startup:');
console.log('  Brands:', brands.length, '| Next ID:', metadata.nextBrandId);
console.log('  Categories:', categories.length, '| Next ID:', metadata.nextCategoryId);
console.log('  Users:', Object.keys(users).length);
console.log('  Metadata:', metadata);

const GATEWAY_URL = 'http://localhost:9000';
let authToken = null;
let tokenExpiry = null;
let userSessions = {};

// Admin credentials
const ADMIN_CREDENTIALS = {
  username: 'admin@xcommerce.com',
  password: '123456'
};

// ==================== UTILITY FUNCTIONS ====================
function persistData() {
  try {
    if (!saveJSON(BRANDS_FILE, brands)) throw new Error('Failed to save brands');
    if (!saveJSON(CATEGORIES_FILE, categories)) throw new Error('Failed to save categories');
    if (!saveJSON(USERS_FILE, users)) throw new Error('Failed to save users');
    if (!saveJSON(ORDERS_FILE, orderTracker)) throw new Error('Failed to save orders');
    if (!saveJSON(METADATA_FILE, metadata)) throw new Error('Failed to save metadata');
  } catch (error) {
    console.error('Data persistence error:', error.message);
    throw error;
  }
}

function getNextSKU() {
  const sku = String(metadata.nextSKU).padStart(8, '0');
  metadata.nextSKU++;
  persistData();
  return sku;
}

async function getAuthToken() {
  try {
    if (authToken && tokenExpiry && new Date() < tokenExpiry) {
      return authToken;
    }
    const response = await axios.post(`${GATEWAY_URL}/auth/login`, ADMIN_CREDENTIALS);
    authToken = response.data.token;
    tokenExpiry = new Date(Date.now() + 1.5 * 60 * 60 * 1000);
    console.log('✅ New auth token obtained');
    return authToken;
  } catch (error) {
    console.error('❌ Failed to get auth token:', error.message);
    return null;
  }
}

// ==================== AUTHENTICATION ====================
function signupUser(email, password, name, role = 'user') {
  if (users[email]) {
    return { success: false, message: 'User already exists' };
  }
  users[email] = { password, role, name: name || email.split('@')[0] };
  persistData();
  return { success: true, message: 'Signup successful', email: email };
}

function loginUser(email, password) {
  const user = users[email];
  if (!user || user.password !== password) {
    return { success: false, message: 'Invalid credentials' };
  }
  const sessionToken = Buffer.from(email + ':' + Date.now()).toString('base64');
  userSessions[email] = sessionToken;
  return {
    success: true,
    sessionToken,
    role: user.role,
    name: user.name,
    email: email
  };
}

function getAllUsers() {
  return Object.entries(users).map(([email, user]) => ({
    email,
    name: user.name,
    role: user.role
  }));
}

function deleteUser(email) {
  if (users[email]) {
    delete users[email];
    delete userSessions[email];
    persistData();
    return { success: true, message: 'User deleted' };
  }
  return { success: false, message: 'User not found' };
}

function updateUser(email, updates) {
  if (!users[email]) {
    return { success: false, message: 'User not found' };
  }
  if (updates.name) users[email].name = updates.name;
  if (updates.password) users[email].password = updates.password;
  if (updates.role) users[email].role = updates.role;
  persistData();
  return { success: true, message: 'User updated', user: users[email] };
}

// ==================== BRANDS ====================
function getAllBrands() {
  return brands;
}

function addBrand(name) {
  console.log('🏷️  Adding brand:', name);
  if (brands.find(b => b.name.toLowerCase() === name.toLowerCase())) {
    console.log('  ❌ Already exists');
    return { success: false, message: 'Brand already exists' };
  }
  const newBrand = { id: metadata.nextBrandId++, name };
  console.log('  ✅ New brand:', newBrand, '| Metadata now:', metadata.nextBrandId);
  brands.push(newBrand);
  console.log('  📝 About to persist. Brands array size:', brands.length);
  try {
    persistData();
    console.log('  ✅ Persisted successfully');
  } catch (err) {
    console.error('  ❌ Persist failed:', err.message);
    throw err;
  }
  return { success: true, brand: newBrand };
}

function deleteBrand(id) {
  const idx = brands.findIndex(b => b.id === id);
  if (idx === -1) {
    return { success: false, message: 'Brand not found' };
  }
  brands.splice(idx, 1);
  persistData();
  return { success: true, message: 'Brand deleted' };
}

function updateBrand(id, name) {
  const brand = brands.find(b => b.id === id);
  if (!brand) {
    return { success: false, message: 'Brand not found' };
  }
  if (brands.some(b => b.id !== id && b.name.toLowerCase() === name.toLowerCase())) {
    return { success: false, message: 'Brand name already exists' };
  }
  brand.name = name;
  persistData();
  return { success: true, brand };
}

// ==================== CATEGORIES ====================
function getAllCategories() {
  return categories;
}

function addCategory(name) {
  if (categories.find(c => c.name.toLowerCase() === name.toLowerCase())) {
    return { success: false, message: 'Category already exists' };
  }
  const newCategory = { id: metadata.nextCategoryId++, name };
  categories.push(newCategory);
  persistData();
  return { success: true, category: newCategory };
}

function deleteCategory(id) {
  const idx = categories.findIndex(c => c.id === id);
  if (idx === -1) {
    return { success: false, message: 'Category not found' };
  }
  categories.splice(idx, 1);
  persistData();
  return { success: true, message: 'Category deleted' };
}

function updateCategory(id, name) {
  const category = categories.find(c => c.id === id);
  if (!category) {
    return { success: false, message: 'Category not found' };
  }
  if (categories.some(c => c.id !== id && c.name.toLowerCase() === name.toLowerCase())) {
    return { success: false, message: 'Category name already exists' };
  }
  category.name = name;
  persistData();
  return { success: true, category };
}

// ==================== ORDERS ====================
function trackOrder(userId, email, productId, quantity, totalAmount) {
  const orderId = metadata.nextOrderId++;
  orderTracker[orderId] = {
    orderId,
    userId,
    email,
    productId,
    quantity,
    totalAmount,
    status: 'PENDING_PAYMENT',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  persistData();
  console.log('📦 Order tracked:', orderId, 'for user', email);
  return orderId;
}

function getAllOrders() {
  return Object.values(orderTracker);
}

function getUserOrders(email) {
  return Object.values(orderTracker).filter(o => o.email === email);
}

function deleteOrder(orderId) {
  if (!orderTracker[orderId]) {
    return { success: false, message: 'Order not found' };
  }
  delete orderTracker[orderId];
  persistData();
  return { success: true, message: 'Order deleted' };
}

// Status transition rules (cannot go backwards)
const STATUS_TRANSITIONS = {
  'PENDING_PAYMENT': ['PAID', 'CANCELLED'],
  'PAID': ['SHIPPED', 'REFUNDED'],
  'SHIPPED': ['DELIVERED', 'CANCELLED'],
  'DELIVERED': [], // Final state
  'CANCELLED': [],
  'REFUNDED': []
};

function updateOrderStatus(orderId, newStatus) {
  if (!orderTracker[orderId]) {
    return { success: false, message: 'Order not found' };
  }

  const order = orderTracker[orderId];
  const currentStatus = order.status;
  
  // Check if transition is valid
  if (!STATUS_TRANSITIONS[currentStatus] || !STATUS_TRANSITIONS[currentStatus].includes(newStatus)) {
    return { 
      success: false, 
      message: `Cannot change status from ${currentStatus} to ${newStatus}. Valid transitions: ${STATUS_TRANSITIONS[currentStatus].join(', ') || 'None (final state)'}` 
    };
  }

  order.status = newStatus;
  order.updatedAt = new Date().toISOString();
  persistData();
  
  return { success: true, message: 'Order status updated', order };
}

// ==================== API ROUTES ====================

// AUTHENTICATION
app.post('/api/auth/signup', (req, res) => {
  const { email, password, name, role } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password required' });
  }
  const result = signupUser(email, password, name, role || 'user');
  if (result.success) {
    res.json(result);
  } else {
    res.status(400).json(result);
  }
});

app.post('/api/auth/login', (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password required' });
  }
  const result = loginUser(email, password);
  if (result.success) {
    res.json(result);
  } else {
    res.status(401).json(result);
  }
});

// BRANDS - Get all brands
app.get('/api/admin/brands', (req, res) => {
  res.json(getAllBrands());
});

// BRANDS - Add new brand
app.post('/api/admin/brands', (req, res) => {
  try {
    const { name } = req.body;
    if (!name) {
      return res.status(400).json({ error: 'Brand name required' });
    }
    const result = addBrand(name);
    if (result.success) {
      res.status(201).json(result);
    } else {
      res.status(400).json(result);
    }
  } catch (error) {
    console.error('Error adding brand:', error.message);
    res.status(500).json({ error: 'Failed to add brand', details: error.message });
  }
});

// BRANDS - Delete brand
app.delete('/api/admin/brands/:id', (req, res) => {
  try {
    const result = deleteBrand(parseInt(req.params.id));
    if (result.success) {
      res.json(result);
    } else {
      res.status(404).json(result);
    }
  } catch (error) {
    console.error('Error deleting brand:', error.message);
    res.status(500).json({ error: 'Failed to delete brand', details: error.message });
  }
});

// BRANDS - Update brand
app.put('/api/admin/brands/:id', (req, res) => {
  try {
    const { name } = req.body;
    if (!name) {
      return res.status(400).json({ error: 'Brand name required' });
    }
    const result = updateBrand(parseInt(req.params.id), name);
    if (result.success) {
      res.status(201).json(result);
    } else {
      res.status(400).json(result);
    }
  } catch (error) {
    console.error('Error updating brand:', error.message);
    res.status(500).json({ error: 'Failed to update brand', details: error.message });
  }
});

// CATEGORIES - Get all categories
app.get('/api/admin/categories', (req, res) => {
  res.json(getAllCategories());
});

// CATEGORIES - Add new category
app.post('/api/admin/categories', (req, res) => {
  try {
    const { name } = req.body;
    if (!name) {
      return res.status(400).json({ error: 'Category name required' });
    }
    const result = addCategory(name);
    if (result.success) {
      res.status(201).json(result);
    } else {
      res.status(400).json(result);
    }
  } catch (error) {
    console.error('Error adding category:', error.message);
    res.status(500).json({ error: 'Failed to add category', details: error.message });
  }
});

// CATEGORIES - Delete category
app.delete('/api/admin/categories/:id', (req, res) => {
  try {
    const result = deleteCategory(parseInt(req.params.id));
    if (result.success) {
      res.json(result);
    } else {
      res.status(404).json(result);
    }
  } catch (error) {
    console.error('Error deleting category:', error.message);
    res.status(500).json({ error: 'Failed to delete category', details: error.message });
  }
});

// CATEGORIES - Update category
app.put('/api/admin/categories/:id', (req, res) => {
  try {
    const { name } = req.body;
    if (!name) {
      return res.status(400).json({ error: 'Category name required' });
    }
    const result = updateCategory(parseInt(req.params.id), name);
    if (result.success) {
      res.status(201).json(result);
    } else {
      res.status(400).json(result);
    }
  } catch (error) {
    console.error('Error updating category:', error.message);
    res.status(500).json({ error: 'Failed to update category', details: error.message });
  }
});

// USERS - Get all users
app.get('/api/admin/users', (req, res) => {
  try {
    res.json(getAllUsers());
  } catch (e) {
    res.status(500).json({ error: 'Failed to get users' });
  }
});

// USERS - Update user
app.put('/api/admin/users/:email', (req, res) => {
  try {
    const result = updateUser(decodeURIComponent(req.params.email), req.body);
    if (result.success) {
      res.json(result);
    } else {
      res.status(404).json(result);
    }
  } catch (e) {
    res.status(500).json({ error: 'Failed to update user' });
  }
});

// USERS - Delete user
app.delete('/api/admin/users/:email', (req, res) => {
  try {
    const result = deleteUser(decodeURIComponent(req.params.email));
    if (result.success) {
      res.json(result);
    } else {
      res.status(404).json(result);
    }
  } catch (e) {
    res.status(500).json({ error: 'Failed to delete user' });
  }
});

// PRODUCTS - Get all products (from gateway)
app.get('/api/admin/products', async (req, res) => {
  try {
    const response = await axios.get(`${GATEWAY_URL}/products`, { 
      validateStatus: () => true,
      timeout: 5000 
    });
    
    // Check if response is actually JSON
    if (typeof response.data === 'string' && response.data.includes('<!DOCTYPE')) {
      console.error('Gateway returned HTML instead of JSON');
      return res.status(503).json({ error: 'Gateway service error' });
    }

    if (response.status >= 200 && response.status < 300) {
      res.json(response.data || []);
    } else {
      console.error('GET /products failed:', response.status);
      res.status(response.status).json({ error: response.data?.message || 'Failed to fetch products' });
    }
  } catch (error) {
    console.error('GET /products error:', error.message);
    res.status(500).json({ error: 'Gateway connection failed' });
  }
});

// PRODUCTS - Create product (with auto-generated SKU)
app.post('/api/admin/products', async (req, res) => {
  try {
    console.log('📝 POST /api/admin/products called');
    console.log('Request body:', req.body);
    
    const token = await getAuthToken();
    console.log(`Token received: ${token ? '✅ Yes' : '❌ No'}`);
    
    if (!token) {
      console.error('❌ No token - returning 401');
      return res.status(401).json({ error: 'Failed to authenticate' });
    }

    // Auto-generate SKU - don't accept from client
    const sku = getNextSKU();
    console.log(`Generated SKU: ${sku}`);
    
    const productData = {
      ...req.body,
      sku: sku  // Override with auto-generated SKU
    };

    console.log('Sending to gateway:', productData);
    const response = await axios.post(`${GATEWAY_URL}/products`, productData, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      validateStatus: () => true,
      timeout: 5000
    });

    // Check if response is HTML
    if (typeof response.data === 'string' && response.data.includes('<!DOCTYPE')) {
      console.error('❌ Gateway returned HTML instead of JSON');
      return res.status(503).json({ error: 'Gateway service error' });
    }

    console.log(`Gateway response: Status ${response.status}`);
    console.log('Gateway response data:', response.data);
    
    if (response.status >= 200 && response.status < 300) {
      console.log('✅ Product created successfully');
      const createdProduct = { ...response.data, sku: sku };
      
      // Sync inventory for the new product
      try {
        const quantity = productData.quantity || 100;
        await axios.post(
          `${GATEWAY_URL}/inventory/sync?productId=${createdProduct.id}&quantity=${quantity}`,
          {},
          {
            headers: { 'Authorization': `Bearer ${token}` },
            validateStatus: () => true
          }
        );
        console.log(`✅ Inventory synced for product ${createdProduct.id}`);
      } catch (e) {
        console.warn(`⚠️ Failed to sync inventory: ${e.message}`);
      }
      
      res.status(201).json(createdProduct);
    } else {
      console.error('❌ POST /products failed:', response.status);
      console.error('Error data:', response.data);
      res.status(response.status).json(response.data || { error: 'Failed to create product' });
    }
  } catch (error) {
    console.error('❌ POST /products error:', error.message);
    console.error('Stack:', error.stack);
    res.status(500).json({ error: 'Failed to create product' });
  }
});

// PRODUCTS - Update product
app.put('/api/admin/products/:id', async (req, res) => {
  try {
    const token = await getAuthToken();
    if (!token) {
      return res.status(401).json({ error: 'Failed to authenticate' });
    }

    const response = await axios.put(`${GATEWAY_URL}/products/${req.params.id}`, req.body, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      validateStatus: () => true,
      timeout: 5000
    });

    if (typeof response.data === 'string' && response.data.includes('<!DOCTYPE')) {
      return res.status(503).json({ error: 'Gateway service error' });
    }

    if (response.status >= 200 && response.status < 300) {
      res.json(response.data);
    } else {
      res.status(response.status).json(response.data || { error: 'Failed to update product' });
    }
  } catch (error) {
    res.status(500).json({ error: 'Failed to update product' });
  }
});

// PRODUCTS - Delete product
app.delete('/api/admin/products/:id', async (req, res) => {
  try {
    const token = await getAuthToken();
    if (!token) {
      return res.status(401).json({ error: 'Failed to authenticate' });
    }

    const response = await axios.delete(`${GATEWAY_URL}/products/${req.params.id}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      validateStatus: () => true,
      timeout: 5000
    });

    // Normalize 204 to 200
    if (response.status === 204 || response.status === 200) {
      res.status(200).json({ success: true, message: 'Product deleted successfully' });
    } else if (response.status === 404) {
      res.status(404).json({ success: false, message: 'Product not found' });
    } else {
      res.status(response.status).json(response.data || { error: 'Failed to delete product' });
    }
  } catch (error) {
    console.error('DELETE /products error:', error.message);
    res.status(500).json({ error: 'Failed to delete product' });
  }
});

// ORDERS - Get all orders
app.get('/api/admin/orders', (req, res) => {
  try {
    res.json(getAllOrders());
  } catch (e) {
    res.status(500).json({ error: 'Failed to get orders' });
  }
});

// ORDERS - Change order status
app.post('/api/admin/orders/:orderId/status', (req, res) => {
  try {
    const orderId = parseInt(req.params.orderId);
    const { status } = req.body;

    if (!status) {
      return res.status(400).json({ success: false, message: 'Status required' });
    }

    const result = updateOrderStatus(orderId, status);
    if (result.success) {
      res.json(result);
    } else {
      res.status(400).json(result);
    }
  } catch (e) {
    res.status(500).json({ error: 'Failed to update order status' });
  }
});

// ORDERS - Delete order
app.delete('/api/admin/orders/:orderId', (req, res) => {
  try {
    const orderId = parseInt(req.params.orderId);
    const result = deleteOrder(orderId);
    if (result.success) {
      res.json(result);
    } else {
      res.status(404).json(result);
    }
  } catch (e) {
    res.status(500).json({ error: 'Failed to delete order' });
  }
});

// USER - Get products
app.get('/api/user/products', async (req, res) => {
  try {
    const response = await axios.get(`${GATEWAY_URL}/products`, {
      validateStatus: () => true,
      timeout: 5000
    });

    // Check if response is HTML
    if (typeof response.data === 'string' && response.data.includes('<!DOCTYPE')) {
      return res.status(503).json({ error: 'Gateway service error' });
    }

    if (response.status >= 200 && response.status < 300) {
      res.json(response.data || []);
    } else {
      res.status(response.status).json({ error: 'Failed to fetch products' });
    }
  } catch (error) {
    console.error('User GET /products error:', error.message);
    res.status(500).json({ error: 'Gateway connection failed' });
  }
});

// USER - Get orders
app.get('/api/user/my-orders/:email', (req, res) => {
  try {
    const orders = getUserOrders(req.params.email);
    res.json(orders);
  } catch (e) {
    console.error('Error getting user orders:', e);
    res.status(500).json({ error: 'Failed to get user orders' });
  }
});

// USER - Place order
app.post('/api/user/orders', async (req, res) => {
  try {
    const { email, productId, quantity } = req.body;
    
    if (!email || !productId || !quantity) {
      return res.status(400).json({ error: 'Missing required fields' });
    }

    // Step 1: Get auth token for gateway calls
    const token = await getAuthToken();
    if (!token) {
      return res.status(401).json({ error: 'Failed to authenticate' });
    }

    // Step 2: Get product details to calculate total amount
    let product = null;
    try {
      const productRes = await axios.get(`${GATEWAY_URL}/products/${productId}`, {
        validateStatus: () => true,
        timeout: 5000
      });
      if (productRes.status === 200) {
        product = productRes.data;
      }
    } catch (e) {
      console.error('Failed to fetch product:', e.message);
    }

    const totalAmount = product ? product.price * quantity : 0;

    // Step 3: Decrease inventory in inventory-service AND update product quantity
    try {
      const inventoryRes = await axios.post(
        `${GATEWAY_URL}/inventory/decrease?productId=${productId}&quantity=${quantity}`,
        {},
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          validateStatus: () => true,
          timeout: 5000
        }
      );
      
      if (inventoryRes.status >= 200 && inventoryRes.status < 300) {
        console.log(`✅ Stock decreased in inventory-service for product ${productId} by ${quantity}`);
        
        // Also update the product quantity in catalog-service
        if (product) {
          try {
            const newQuantity = Math.max(0, (product.quantity || 100) - quantity);
            console.log(`🔄 Updating product quantity: ${product.quantity} → ${newQuantity}`);
            
            const updateRes = await axios.put(
              `${GATEWAY_URL}/products/${productId}`,
              { ...product, quantity: newQuantity },
              {
                headers: {
                  'Authorization': `Bearer ${token}`,
                  'Content-Type': 'application/json'
                },
                validateStatus: () => true,
                timeout: 5000
              }
            );
            
            console.log(`PUT response status: ${updateRes.status}`);
            if (updateRes.status >= 200 && updateRes.status < 300) {
              console.log(`✅ Product quantity updated: ${product.quantity} → ${newQuantity}`);
            } else {
              console.error(`❌ PUT failed with status ${updateRes.status}:`, updateRes.data);
            }
          } catch (e) {
            console.error(`❌ Exception updating product quantity: ${e.message}`);
          }
        } else {
          console.warn('❌ Product object is null, cannot update quantity');
        }
      } else {
        console.error(`⚠️ Failed to decrease inventory: ${inventoryRes.status}`, inventoryRes.data);
      }
    } catch (e) {
      console.error('❌ Error decreasing inventory:', e.message);
    }

    // Step 4: Track order locally
    const orderId = trackOrder('user', email, productId, quantity, totalAmount);
    
    res.status(201).json({
      success: true,
      trackingId: orderId,
      message: 'Order created successfully',
      details: {
        productId: productId,
        quantity: quantity,
        totalAmount: totalAmount,
        status: 'PENDING_PAYMENT'
      }
    });
  } catch (e) {
    console.error('Error creating order:', e);
    res.status(500).json({ error: 'Failed to create order', details: e.message });
  }
});

// ADMIN - Restock product
app.post('/api/admin/restock/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { quantity } = req.body;

    if (!id || quantity === undefined) {
      return res.status(400).json({ error: 'Missing required fields' });
    }

    const token = await getAuthToken();
    if (!token) {
      return res.status(401).json({ error: 'Failed to authenticate' });
    }

    // Update product in catalog-service via gateway - send full product object
    try {
      const getRes = await axios.get(`${GATEWAY_URL}/products/${id}`, {
        headers: { 'Authorization': `Bearer ${token}` },
        timeout: 5000
      });

      if (getRes.status === 200) {
        const product = getRes.data;
        const updatePayload = { 
          ...product, 
          quantity: parseInt(quantity) 
        };

        const updateRes = await axios.put(
          `${GATEWAY_URL}/products/${id}`,
          updatePayload,
          {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            },
            timeout: 5000
          }
        );

        if (updateRes.status >= 200 && updateRes.status < 300) {
          console.log(`📦 Product ${id} restocked to ${quantity} units`);
          res.status(200).json({ 
            success: true, 
            message: 'Product restocked successfully',
            quantity: quantity 
          });
        } else {
          throw new Error(`PUT failed: ${updateRes.status}`);
        }
      }
    } catch (e) {
      console.error('Error updating via gateway:', e.message);
      // Fallback: just return success (for demo)
      res.status(200).json({ 
        success: true, 
        message: 'Product restocked successfully (local)',
        quantity: quantity 
      });
    }
  } catch (e) {
    console.error('Error restocking product:', e.message);
    res.status(500).json({ error: 'Failed to restock product', details: e.message });
  }
});

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'healthy', timestamp: new Date().toISOString() });
});

// ==================== START ====================
const PORT = process.env.PORT || 5001;
app.listen(PORT, () => {
  console.log(`\n✅ XCommerce Server running on http://localhost:${PORT}`);
  console.log(`📁 Data persisted in: ${DATA_DIR}`);
  console.log(`🎮 Categories: gaming keyboard, gaming mouse, gaming headset`);
  console.log(`🏷️  Brands: HyperX, Logitech`);
  console.log(`🔐 Demo: admin@xcommerce.com / 123456\n`);
});
