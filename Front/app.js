
const API_BASE_URL = 'https://sistema-subastas-online.onrender.com';

const state = {
    token: localStorage.getItem('jwt_token') || null,
    user: null, 
    activeView: 'catalog',
    sellerActiveTab: 'products',
    buyerActiveTab: 'won',
    adminActiveTab: 'users',
    catalogSearchQuery: '',
    catalogSelectedCategory: '',
    catalogSelectedState: 'ACTIVA',

    categories: [],
    products: [],
    auctions: [],
    activeAuction: null,
    activeAuctionBids: [],
    allUsers: [],
    disputes: [],
    notifications: [],

    notificationsInterval: null,
    auctionTimerInterval: null
};

window.addEventListener('DOMContentLoaded', async () => {
    logConsole('info', 'SYSTEM', 'Inicializando frontend...', null, null);

    state.categories = [
        { id: 1, nombre: 'Electrónica y Tecnología' },
        { id: 2, nombre: 'Vehículos y Accesorios' },
        { id: 3, nombre: 'Hogar y Muebles' },
        { id: 4, nombre: 'Moda y Vestimenta' },
        { id: 5, nombre: 'Coleccionables y Arte' }
    ];
    populateCategoryDropdowns();

    let sessionRestored = false;
    if (state.token) {
        sessionRestored = await restoreSession();
        if (!sessionRestored) {
            handleLogout();
        }
    } else {
        updateNavbar();
    }

    if (sessionRestored) {
        await loadCatalog();
        navigateTo('catalog');
        startNotificationsPolling();
    } else {
        navigateTo('auth');
    }
});

async function restoreSession() {
    try {
        const payload = decodeJwt(state.token);
        if (!payload || (payload.exp && payload.exp * 1000 < Date.now())) {
            logConsole('error', 'JWT', 'El token ha expirado.', null, null);
            showToast('Su sesión ha expirado, por favor ingrese nuevamente.', 'warning');
            return false;
        }

        const username = payload.sub;
        const response = await apiCall(`/api/usuarios/username/${username}`, { method: 'GET' });
        if (response.ok) {
            state.user = await response.json();
            logConsole('success', 'PROFILE', `Sesión restaurada para: ${state.user.username} (Rol: ${state.user.rol})`, null, state.user);
            updateNavbar();
            return true;
        } else {
            return false;
        }
    } catch (error) {
        console.error('Error restoring session:', error);
        return false;
    }
}

function decodeJwt(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        return null;
    }
}

async function apiCall(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;

    if (!options.headers) {
        options.headers = {};
    }
    
    if (state.token) {
        options.headers['Authorization'] = `Bearer ${state.token}`;
    }
    
    if (options.body && !(options.body instanceof FormData)) {
        options.headers['Content-Type'] = 'application/json';
        if (typeof options.body === 'object') {
            options.body = JSON.stringify(options.body);
        }
    }
    
    const method = options.method || 'GET';
    const startTime = Date.now();
    
    try {
        const response = await fetch(url, options);
        const duration = Date.now() - startTime;

        let responseData = null;
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            
            const clonedResponse = response.clone();
            responseData = await clonedResponse.json();
        }

        logConsole(
            response.ok ? 'success' : 'error',
            method,
            endpoint,
            response.status,
            responseData,
            options.body ? JSON.parse(options.body) : null,
            duration
        );

        if (response.status === 401 && state.token) {
            showToast('No autorizado o sesión expirada.', 'error');
            handleLogout();
        }
        
        return response;
    } catch (error) {
        const duration = Date.now() - startTime;
        logConsole('error', method, endpoint, 'FETCH_FAILED', { error: error.message }, options.body ? JSON.parse(options.body) : null, duration);
        showToast('Error de conexión con el servidor backend.', 'error');
        throw error;
    }
}

function navigateTo(viewId) {
    if (state.auctionTimerInterval) {
        clearInterval(state.auctionTimerInterval);
        state.auctionTimerInterval = null;
    }

    // Force redirection to auth view if user is not authenticated
    if (!state.user && viewId !== 'auth') {
        if (state.activeView && state.activeView !== 'auth') {
            showToast('Por favor, inicia sesión para continuar.', 'warning');
        }
        viewId = 'auth';
    }

    document.querySelectorAll('.view').forEach(view => view.classList.remove('active'));

    const targetView = document.getElementById(`view-${viewId}`);
    if (targetView) {
        targetView.classList.add('active');
        state.activeView = viewId;

        document.querySelectorAll('.nav-link').forEach(link => {
            if (link.getAttribute('onclick') && link.getAttribute('onclick').includes(`'${viewId}'`)) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });

        if (viewId === 'catalog') {
            loadCatalog();
        } else if (viewId === 'seller') {
            loadSellerPanel();
        } else if (viewId === 'buyer') {
            loadBuyerPanel();
        } else if (viewId === 'admin') {
            loadAdminPanel();
        } else if (viewId === 'notifications') {
            loadNotifications();
        }
        
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function updateNavbar() {
    const navbarLinks = document.getElementById('navbar-links');
    const authNavSection = document.getElementById('auth-nav-section');

    navbarLinks.innerHTML = '';
    authNavSection.innerHTML = '';

    if (state.user) {
        navbarLinks.innerHTML += `<a class="nav-link active" onclick="navigateTo('catalog')"><i class="fa-solid fa-house"></i> Catálogo</a>`;
        
        if (state.user.rol === 'ROLE_SELLER') {
            navbarLinks.innerHTML += `<a class="nav-link" onclick="navigateTo('seller')"><i class="fa-solid fa-tags"></i> Mi Panel Vendedor</a>`;
        }
        
        if (state.user.rol === 'ROLE_USER') {
            navbarLinks.innerHTML += `<a class="nav-link" onclick="navigateTo('buyer')"><i class="fa-solid fa-cart-shopping"></i> Mi Panel Comprador</a>`;
        }
        
        if (state.user.rol === 'ROLE_ADMIN') {
            navbarLinks.innerHTML += `<a class="nav-link" onclick="navigateTo('admin')"><i class="fa-solid fa-user-shield"></i> Consola Admin</a>`;
        }

        const unreadCount = state.notifications.filter(n => !n.leida).length;
        const badgeHtml = unreadCount > 0 ? `<span class="notification-badge">${unreadCount}</span>` : '';
        
        navbarLinks.innerHTML += `
            <a class="nav-link notification-bell-container" onclick="navigateTo('notifications')">
                <i class="fa-solid fa-bell"></i> Notificaciones ${badgeHtml}
            </a>
        `;

        authNavSection.innerHTML = `
            <div class="user-widget">
                <div class="user-avatar">${state.user.username.substring(0,2).toUpperCase()}</div>
                <div class="user-info-text">
                    <span class="user-name">${state.user.username}</span>
                    <span class="user-role">${state.user.rol === 'ROLE_ADMIN' ? 'Admin' : (state.user.rol === 'ROLE_SELLER' ? 'Vendedor' : 'Comprador')}</span>
                </div>
                <button class="btn btn-secondary btn-sm" onclick="handleLogout()" title="Cerrar Sesión" style="border-radius: 50%; padding: 0.4rem; width: 28px; height: 28px; display:flex; align-items:center; justify-content:center;">
                    <i class="fa-solid fa-right-from-bracket"></i>
                </button>
            </div>
        `;
    } else {
        authNavSection.innerHTML = `
            <button class="btn btn-primary btn-sm" onclick="navigateTo('auth'); switchAuthTab('login');">
                <i class="fa-solid fa-sign-in"></i> Acceder / Registrarse
            </button>
        `;
    }
}

async function handleLogin(event) {
    event.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    
    try {
        const response = await apiCall('/api/auth/login', {
            method: 'POST',
            body: { username, password }
        });
        
        if (response.ok) {
            const data = await response.json();
            state.token = data.token;
            localStorage.setItem('jwt_token', data.token);

            const profileRestored = await restoreSession();
            if (profileRestored) {
                showToast(`¡Bienvenido de vuelta, ${state.user.username}!`, 'success');
                startNotificationsPolling();
                navigateTo('catalog');
            } else {
                showToast('Error al cargar perfil de usuario.', 'error');
                handleLogout();
            }
        } else if (response.status === 401) {
            showToast('Credenciales incorrectas.', 'error');
        } else {
            showToast('Ocurrió un error al intentar ingresar.', 'error');
        }
    } catch (e) {
        console.error(e);
    }
}

async function handleRegister(event) {
    event.preventDefault();
    const username = document.getElementById('register-username').value.trim();
    const email = document.getElementById('register-email').value.trim();
    const password = document.getElementById('register-password').value;
    const rolSolicitado = document.getElementById('register-role').value;
    
    try {
        const response = await apiCall('/api/auth/register', {
            method: 'POST',
            body: { username, email, password, rolSolicitado }
        });
        
        if (response.status === 201) {
            showToast('Registro exitoso. Ya puedes iniciar sesión.', 'success');
            
            document.getElementById('form-register').reset();
            switchAuthTab('login');
        } else if (response.status === 409) {
            showToast('Nombre de usuario o correo electrónico ya registrados.', 'error');
        } else if (response.status === 400) {
            const errData = await response.json();
            showToast(errData.message || 'Datos de registro inválidos.', 'warning');
        } else {
            showToast('Error en el registro de usuario.', 'error');
        }
    } catch (e) {
        console.error(e);
    }
}

function handleLogout() {
    logConsole('info', 'LOGOUT', 'Sesión cerrada por el usuario.', null, null);
    state.token = null;
    state.user = null;
    state.products = [];
    state.notifications = [];
    localStorage.removeItem('jwt_token');
    
    if (state.notificationsInterval) {
        clearInterval(state.notificationsInterval);
        state.notificationsInterval = null;
    }
    
    updateNavbar();
    navigateTo('auth');
    showToast('Sesión cerrada correctamente.', 'info');
}

function switchAuthTab(tab) {
    const loginForm = document.getElementById('auth-login-form');
    const registerForm = document.getElementById('auth-register-form');
    const tabs = document.querySelectorAll('#view-auth .tab-btn');
    
    if (tab === 'login') {
        loginForm.style.display = 'block';
        registerForm.style.display = 'none';
        tabs[0].classList.add('active');
        tabs[1].classList.remove('active');
    } else {
        loginForm.style.display = 'none';
        registerForm.style.display = 'block';
        tabs[0].classList.remove('active');
        tabs[1].classList.add('active');
    }
}

function populateCategoryDropdowns() {
    const catalogCatSelect = document.getElementById('catalog-filter-category');
    const prodCatSelect = document.getElementById('product-category');
    
    catalogCatSelect.innerHTML = '<option value="">Todas las Categorías</option>';
    prodCatSelect.innerHTML = '';
    
    state.categories.forEach(cat => {
        catalogCatSelect.innerHTML += `<option value="${cat.id}">${cat.nombre}</option>`;
        prodCatSelect.innerHTML += `<option value="${cat.id}">${cat.nombre}</option>`;
    });
}

async function loadCatalog() {
    try {
        let endpoint = '/api/subastas';
        const selectState = state.catalogSelectedState;

        if (selectState && selectState !== 'ALL') {
            endpoint = `/api/subastas/estado/${selectState}`;
        }
        
        const response = await apiCall(endpoint, { method: 'GET' });
        
        if (response.ok) {
            state.auctions = await response.json();
            filterCatalog();
        } else {
            showToast('Error al cargar subastas del catálogo.', 'error');
        }
    } catch (e) {
        console.error(e);
    }
}

function filterCatalog() {
    const searchVal = document.getElementById('catalog-search').value.toLowerCase().trim();
    const catVal = document.getElementById('catalog-filter-category').value;
    
    const filtered = state.auctions.filter(auc => {
        const matchesSearch = auc.titulo.toLowerCase().includes(searchVal) || 
                              (auc.descripcion && auc.descripcion.toLowerCase().includes(searchVal));
        const matchesCategory = !catVal || (auc.producto && auc.producto.categoriaId === parseInt(catVal));
        return matchesSearch && matchesCategory;
    });
    
    renderCatalogGrid(filtered);
}

function renderCatalogGrid(items) {
    const grid = document.getElementById('catalog-grid');
    const emptyState = document.getElementById('catalog-empty-state');
    grid.innerHTML = '';
    
    if (items.length === 0) {
        emptyState.style.display = 'block';
        return;
    }
    
    emptyState.style.display = 'none';
    
    items.forEach(auc => {
        const timeBadge = getAuctionTimeRemainingBadge(auc);
        const stateBadge = getAuctionStateBadge(auc.estado);
        const finalPrice = auc.montoActual || auc.precioBase;
        
        grid.innerHTML += `
            <div class="auction-card">
                <div class="card-header">
                    <span class="badge badge-default">${auc.producto ? auc.producto.categoriaNombre || 'General' : 'General'}</span>
                    ${stateBadge}
                </div>
                <div class="card-body">
                    <h3 class="card-title">${auc.titulo}</h3>
                    <p class="card-desc">${auc.descripcion || 'Sin descripción adicional.'}</p>
                    
                    <div class="card-meta">
                        <div class="meta-item">
                            <span class="meta-label">Precio Actual</span>
                            <span class="meta-val price">$${finalPrice.toLocaleString()}</span>
                        </div>
                        <div class="meta-item">
                            <span class="meta-label">Pujas</span>
                            <span class="meta-val">${auc.cantidadPujas || 0}</span>
                        </div>
                    </div>
                </div>
                <div class="card-footer" style="display:flex; justify-content:space-between; align-items:center;">
                    ${timeBadge}
                    <button class="btn btn-primary btn-sm" onclick="viewAuctionDetail(${auc.id})">
                        Ver Detalles <i class="fa-solid fa-arrow-right"></i>
                    </button>
                </div>
            </div>
        `;
    });
}

function getAuctionTimeRemainingBadge(auc) {
    if (auc.estado === 'BORRADOR' || auc.estado === 'PUBLICADA') {
        return `<span class="time-remaining"><i class="fa-regular fa-clock"></i> Inicia: ${formatDate(auc.fechaInicio)}</span>`;
    }
    
    if (auc.estado === 'ACTIVA') {
        const timeDiff = new Date(auc.fechaCierre) - new Date();
        if (timeDiff <= 0) {
            return `<span class="time-remaining critical"><i class="fa-solid fa-clock"></i> Expirada</span>`;
        }

        const hours = Math.floor(timeDiff / (1000 * 60 * 60));
        const minutes = Math.floor((timeDiff % (1000 * 60 * 60)) / (1000 * 60));
        
        let display = '';
        if (hours > 24) {
            display = `${Math.floor(hours / 24)}d ${hours % 24}h`;
        } else {
            display = `${hours}h ${minutes}m`;
        }
        
        const isCritical = timeDiff < (1000 * 60 * 60); 
        return `<span class="time-remaining ${isCritical ? 'critical' : ''}"><i class="fa-solid fa-hourglass-half"></i> Cierra en: ${display}</span>`;
    }
    
    return `<span class="time-remaining"><i class="fa-solid fa-lock"></i> Cerrada</span>`;
}

function getAuctionStateBadge(estado) {
    switch (estado) {
        case 'BORRADOR': return `<span class="badge badge-default">Borrador</span>`;
        case 'PUBLICADA': return `<span class="badge badge-info">Publicada</span>`;
        case 'ACTIVA': return `<span class="badge badge-success" style="animation: blink 2s infinite;"><i class="fa-solid fa-circle" style="font-size: 0.5rem; margin-right: 0.25rem;"></i> Activa</span>`;
        case 'FINALIZADA': return `<span class="badge badge-default">Finalizada</span>`;
        case 'ADJUDICADA': return `<span class="badge badge-primary"><i class="fa-solid fa-trophy"></i> Adjudicada</span>`;
        case 'EN_DISPUTA': return `<span class="badge badge-danger" style="animation: blink 1s infinite;"><i class="fa-solid fa-triangle-exclamation"></i> En Disputa</span>`;
        default: return `<span class="badge badge-default">${estado}</span>`;
    }
}

async function viewAuctionDetail(id) {
    state.activeAuctionId = id;
    navigateTo('detail');
    
    await loadAuctionDetailData();

    if (state.auctionTimerInterval) {
        clearInterval(state.auctionTimerInterval);
    }
    
    state.auctionTimerInterval = setInterval(() => {
        if (state.activeAuction && state.activeAuction.estado === 'ACTIVA') {
            updateDetailCountdown();
        }
    }, 1000);
}

async function loadAuctionDetailData() {
    try {
        const id = state.activeAuctionId;
        const response = await apiCall(`/api/subastas/${id}`, { method: 'GET' });
        
        if (response.ok) {
            state.activeAuction = await response.json();

            const bidsResponse = await apiCall(`/api/pujas/subasta/${id}`, { method: 'GET' });
            if (bidsResponse.ok) {
                state.activeAuctionBids = await bidsResponse.json();
            } else {
                state.activeAuctionBids = [];
            }
            
            renderAuctionDetail();
        } else {
            showToast('No se pudo obtener detalles de la subasta.', 'error');
            navigateTo('catalog');
        }
    } catch (e) {
        console.error(e);
    }
}

function renderAuctionDetail() {
    const container = document.getElementById('detail-auction-container');
    const auc = state.activeAuction;
    const bids = state.activeAuctionBids;
    
    if (!auc) return;
    
    const isSellerOfAuction = state.user && state.user.id === auc.vendedorId;
    const isLoggedIn = !!state.user;
    const isBuyer = state.user && state.user.rol === 'ROLE_USER';

    const currentPrice = auc.montoActual || auc.precioBase;
    const minRequiredBid = currentPrice + auc.incrementoMinimoPuja;

    const stateBadge = getAuctionStateBadge(auc.estado);
    const startStr = formatDate(auc.fechaInicio);
    const endStr = formatDate(auc.fechaCierre);

    let mainHtml = `
        <div class="detail-main">
            <div class="detail-header">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <span class="badge badge-primary">${auc.producto ? auc.producto.categoriaNombre || 'Categoría General' : 'General'}</span>
                    ${stateBadge}
                </div>
                <h1 class="detail-title">${auc.titulo}</h1>
                <p style="color: var(--text-muted); font-size: 0.9rem;">
                    <i class="fa-solid fa-user"></i> Vendedor ID: ${auc.vendedorId} | <i class="fa-solid fa-barcode"></i> Producto ID: ${auc.productoId}
                </p>
            </div>
            
            <p class="detail-desc">${auc.descripcion || 'Sin descripción detallada por el vendedor.'}</p>
            
            <div class="detail-grid-info">
                <div class="detail-info-card">
                    <span class="meta-label">Precio Base</span>
                    <span class="meta-val">$${auc.precioBase.toLocaleString()}</span>
                </div>
                <div class="detail-info-card">
                    <span class="meta-label">Incremento Mínimo</span>
                    <span class="meta-val">$${auc.incrementoMinimoPuja.toLocaleString()}</span>
                </div>
                <div class="detail-info-card">
                    <span class="meta-label">Fecha de Inicio</span>
                    <span class="meta-val" style="font-size:0.9rem; font-weight:500;">${startStr}</span>
                </div>
                <div class="detail-info-card">
                    <span class="meta-label">Fecha de Cierre</span>
                    <span class="meta-val" style="font-size:0.9rem; font-weight:500;" id="detail-date-end-display">${endStr}</span>
                </div>
            </div>
            
            <!-- Bid History -->
            <div class="bid-history">
                <h3>Historial de Pujas (${bids.length})</h3>
                <div class="bid-list">
                    ${bids.length === 0 ? `
                        <div style="text-align:center; padding: 2rem; color: var(--text-muted);">
                            <i class="fa-solid fa-comments-dollar" style="font-size:2rem; margin-bottom: 0.5rem;"></i>
                            <p>No hay ofertas registradas aún. ¡Sé el primero en ofertar!</p>
                        </div>
                    ` : bids.map((b, idx) => {
                        const isTop = idx === 0;
                        return `
                            <div class="bid-item ${isTop ? 'top-bid' : ''}">
                                <div class="bid-item-user">
                                    <div class="user-avatar" style="width:24px; height:24px; font-size:0.7rem;">
                                        ${isTop ? '👑' : 'U'}
                                    </div>
                                    <div>
                                        <span style="font-weight:600; color: var(--text-bright)">Comprador ID: ${b.compradorId || b.usuarioId || 'Anon'}</span>
                                        <div class="bid-item-date">${formatDate(b.fechaPuja || b.fechaHora)}</div>
                                    </div>
                                </div>
                                <span class="bid-item-val ${isTop ? 'success' : ''}">$${b.monto.toLocaleString()}</span>
                            </div>
                        `;
                    }).join('')}
                </div>
            </div>
        </div>
    `;

    let sidebarHtml = '';
    
    if (auc.estado === 'ACTIVA') {
        if (!isLoggedIn) {
            sidebarHtml = `
                <div class="place-bid-card" style="text-align:center;">
                    <p style="margin-bottom:1.5rem; color: var(--text-muted);">Debes iniciar sesión para realizar ofertas.</p>
                    <button class="btn btn-primary" style="width:100%" onclick="navigateTo('auth')">Iniciar Sesión</button>
                </div>
            `;
        } else if (isSellerOfAuction) {
            sidebarHtml = `
                <div class="place-bid-card">
                    <div class="place-bid-title">Estado de tu Subasta</div>
                    <div class="current-price-hero">
                        $${currentPrice.toLocaleString()}
                        <span>Monto Actual</span>
                    </div>
                    <p style="color: var(--text-muted); font-size: 0.9rem; margin-bottom: 1.5rem;">
                        Como vendedor, no puedes auto-ofertar. Puedes forzar el cierre anticipado de la subasta.
                    </p>
                    <button class="btn btn-danger" style="width:100%" onclick="closeAuctionManual(${auc.id})">
                        <i class="fa-solid fa-lock"></i> Cerrar Subasta Ahora
                    </button>
                </div>
            `;
        } else if (isBuyer) {
            sidebarHtml = `
                <div class="place-bid-card">
                    <div class="place-bid-title">Ofertar por este Producto</div>
                    <div class="current-price-hero" id="detail-current-price-hero">
                        $${currentPrice.toLocaleString()}
                        <span>Monto Actual</span>
                    </div>
                    <div id="detail-timer-countdown" class="time-remaining critical" style="margin-bottom:1.5rem; font-size:1.1rem; justify-content:center;">
                        Cargando temporizador...
                    </div>
                    
                    <form id="form-place-bid" onsubmit="handlePlaceBid(event)">
                        <div class="form-group">
                            <label for="bid-amount">Monto de Puja ($)</label>
                            <input type="number" id="bid-amount" class="form-control" step="0.01" min="${minRequiredBid}" value="${minRequiredBid}" required>
                            <span style="font-size:0.75rem; color: var(--text-muted); display:block; margin-top:0.25rem;">
                                Oferta mínima requerida: $${minRequiredBid.toLocaleString()}
                            </span>
                        </div>
                        <button type="submit" class="btn btn-primary" style="width:100%">
                            <i class="fa-solid fa-gavel"></i> Confirmar Oferta
                        </button>
                    </form>
                </div>
            `;
        } else {
            
            sidebarHtml = `
                <div class="place-bid-card">
                    <div class="place-bid-title">Moderación Administrativa</div>
                    <div class="current-price-hero">
                        $${currentPrice.toLocaleString()}
                    </div>
                    <p style="color: var(--text-muted); font-size: 0.9rem; margin-bottom: 1.5rem;">
                        Estás visualizando esta subasta como Administrador. Puedes forzar el cierre técnico.
                    </p>
                    <button class="btn btn-danger" style="width:100%" onclick="closeAuctionManual(${auc.id})">
                        Cerrar Subasta (Admin Force)
                    </button>
                </div>
            `;
        }
    } else if (auc.estado === 'ADJUDICADA' || auc.estado === 'FINALIZADA') {
        const isWinner = state.user && state.user.id === auc.ganadorId;
        
        let disputeBtnHtml = '';
        if (isLoggedIn && (isSellerOfAuction || isWinner)) {
            disputeBtnHtml = `
                <button class="btn btn-danger" style="width:100%; margin-top:1rem;" onclick="openOpenDisputeModal(${auc.id})">
                    <i class="fa-solid fa-triangle-exclamation"></i> Abrir Disputa / Reclamación
                </button>
            `;
        }
        
        sidebarHtml = `
            <div class="place-bid-card" style="border-color: var(--primary);">
                <div class="place-bid-title" style="color:var(--primary);"><i class="fa-solid fa-trophy"></i> Subasta Cerrada</div>
                <div class="current-price-hero">
                    $${(auc.precioFinal || currentPrice).toLocaleString()}
                    <span>Precio Final</span>
                </div>
                
                <div style="background-color: var(--bg-base); padding: 1rem; border-radius: var(--radius-md); border:1px solid var(--border-dim); font-size: 0.9rem;">
                    <div><strong>Vendedor ID:</strong> ${auc.vendedorId}</div>
                    <div><strong>Ganador ID:</strong> ${auc.ganadorId || 'Ninguno'}</div>
                    ${auc.fechaAdjudicacion ? `<div><strong>Adjudicación:</strong> ${formatDate(auc.fechaAdjudicacion)}</div>` : ''}
                </div>
                
                ${isWinner ? `<div style="margin-top:1rem; padding:0.5rem; background-color: var(--success-glow); border:1px solid var(--success); border-radius:var(--radius-sm); text-align:center; font-weight:700; color:var(--success);">🎉 ¡Felicidades! Ganaste esta subasta</div>` : ''}
                
                ${disputeBtnHtml}
            </div>
        `;
    } else if (auc.estado === 'BORRADOR' || auc.estado === 'PUBLICADA') {
        
        let actionBtnHtml = '';
        if (isSellerOfAuction && auc.estado === 'BORRADOR') {
            actionBtnHtml = `
                <button class="btn btn-primary" style="width:100%; margin-top:1rem;" onclick="updateAuctionStateDirect(${auc.id}, 'PUBLICADA')">
                    Publicar Subasta
                </button>
            `;
        } else if (isSellerOfAuction && auc.estado === 'PUBLICADA') {
            actionBtnHtml = `
                <button class="btn btn-success" style="width:100%; margin-top:1rem;" onclick="updateAuctionStateDirect(${auc.id}, 'ACTIVA')">
                    Iniciar Subasta Inmediatamente
                </button>
            `;
        }
        
        sidebarHtml = `
            <div class="place-bid-card">
                <div class="place-bid-title">Subasta en Espera</div>
                <div class="current-price-hero">
                    $${auc.precioBase.toLocaleString()}
                    <span>Precio Base</span>
                </div>
                <div style="font-size:0.9rem; color: var(--text-muted);">
                    <div><strong>Apertura:</strong> ${startStr}</div>
                    <div><strong>Cierre:</strong> ${endStr}</div>
                </div>
                ${actionBtnHtml}
            </div>
        `;
    } else if (auc.estado === 'EN_DISPUTA') {
        sidebarHtml = `
            <div class="place-bid-card" style="border-color: var(--danger);">
                <div class="place-bid-title" style="color:var(--danger);"><i class="fa-solid fa-triangle-exclamation"></i> En Disputa</div>
                <div class="current-price-hero">
                    $${currentPrice.toLocaleString()}
                </div>
                <p style="color:var(--text-muted); font-size:0.9rem;">
                    Esta subasta se encuentra bloqueada administrativamente por un proceso de mediación. Los fondos o el producto están retenidos.
                </p>
                ${state.user && state.user.rol === 'ROLE_ADMIN' ? `
                    <button class="btn btn-primary" style="width:100%; margin-top: 1rem;" onclick="navigateTo('admin'); switchAdminTab('disputes');">
                        Ir a Consola Administrativa
                    </button>
                ` : ''}
            </div>
        `;
    }
    
    container.innerHTML = `
        ${mainHtml}
        <div class="detail-sidebar">
            ${sidebarHtml}
        </div>
    `;
}

function updateDetailCountdown() {
    const timerDisplay = document.getElementById('detail-timer-countdown');
    if (!timerDisplay || !state.activeAuction) return;
    
    const timeDiff = new Date(state.activeAuction.fechaCierre) - new Date();
    
    if (timeDiff <= 0) {
        timerDisplay.innerHTML = `<i class="fa-solid fa-clock"></i> ¡TIEMPO EXPIRADO!`;
        timerDisplay.classList.add('critical');
        
        loadAuctionDetailData();
        clearInterval(state.auctionTimerInterval);
        state.auctionTimerInterval = null;
        return;
    }
    
    const hours = Math.floor(timeDiff / (1000 * 60 * 60));
    const minutes = Math.floor((timeDiff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((timeDiff % (1000 * 60)) / 1000);
    
    const hoursStr = String(hours).padStart(2, '0');
    const minutesStr = String(minutes).padStart(2, '0');
    const secondsStr = String(seconds).padStart(2, '0');
    
    timerDisplay.innerHTML = `<i class="fa-solid fa-hourglass-half"></i> Cierre: ${hoursStr}:${minutesStr}:${secondsStr}`;
    
    if (timeDiff < (1000 * 60 * 15)) { 
        timerDisplay.classList.add('critical');
    } else {
        timerDisplay.classList.remove('critical');
    }
}

async function handlePlaceBid(event) {
    event.preventDefault();
    if (!state.user) {
        showToast('Debe estar autenticado.', 'warning');
        return;
    }
    
    const amount = parseFloat(document.getElementById('bid-amount').value);
    
    try {
        const response = await apiCall('/api/pujas', {
            method: 'POST',
            body: {
                subastaId: state.activeAuctionId,
                compradorId: state.user.id,
                monto: amount
            }
        });
        
        if (response.status === 201) {
            showToast('¡Tu puja ha sido registrada con éxito!', 'success');
            await loadAuctionDetailData();
        } else {
            const errMsg = await getErrorMessage(response, 'No se pudo procesar la oferta.');
            showToast(errMsg, 'error');
        }
    } catch (e) {
        console.error(e);
    }
}

async function closeAuctionManual(id) {
    if (!confirm('¿Estás seguro de que deseas cerrar esta subasta manualmente?')) return;
    
    try {
        const response = await apiCall(`/api/subastas/${id}/cerrar`, { method: 'POST' });
        if (response.ok) {
            showToast('Subasta cerrada con éxito.', 'success');
            if (state.activeView === 'detail') {
                await loadAuctionDetailData();
            } else if (state.activeView === 'seller') {
                await loadSellerPanel();
            }
        } else {
            showToast('Error al cerrar la subasta.', 'error');
        }
    } catch(e) {
        console.error(e);
    }
}

async function updateAuctionStateDirect(id, newState) {
    try {
        
        const currentRes = await apiCall(`/api/subastas/${id}`, { method: 'GET' });
        if (!currentRes.ok) return;
        const current = await currentRes.json();
        
        const payload = {
            vendedorId: current.vendedorId,
            productoId: current.productoId,
            precioBase: current.precioBase,
            incrementoMinimoPuja: current.incrementoMinimoPuja,
            titulo: current.titulo,
            descripcion: current.descripcion,
            fechaInicio: current.fechaInicio,
            fechaCierre: current.fechaCierre,
            estado: newState
        };
        
        const response = await apiCall(`/api/subastas/${id}`, {
            method: 'PUT',
            body: payload
        });
        
        if (response.ok) {
            showToast(`Subasta actualizada a: ${newState}`, 'success');
            await loadAuctionDetailData();
        } else {
            showToast('Error al actualizar estado de subasta.', 'error');
        }
    } catch(e) {
        console.error(e);
    }
}

async function loadSellerPanel() {
    if (!state.user || state.user.rol !== 'ROLE_SELLER') {
        navigateTo('catalog');
        return;
    }
    
    switchSellerTab(state.sellerActiveTab);

    try {
        const prodRes = await apiCall(`/api/productos/vendedor/${state.user.id}`, { method: 'GET' });
        if (prodRes.ok) {
            state.products = await prodRes.json();
            renderSellerProducts();
        }

        const aucRes = await apiCall(`/api/subastas/vendedor/${state.user.id}`, { method: 'GET' });
        if (aucRes.ok) {
            state.auctions = await aucRes.json();
            renderSellerAuctions();
        }
    } catch(e) {
        console.error(e);
    }
}

function switchSellerTab(tab) {
    state.sellerActiveTab = tab;
    
    const tabProducts = document.getElementById('tab-seller-products');
    const tabAuctions = document.getElementById('tab-seller-auctions');
    const contentProducts = document.getElementById('seller-tab-products');
    const contentAuctions = document.getElementById('seller-tab-auctions');
    
    if (tab === 'products') {
        tabProducts.classList.add('active');
        tabAuctions.classList.remove('active');
        contentProducts.classList.add('active');
        contentAuctions.classList.remove('active');
    } else {
        tabProducts.classList.remove('active');
        tabAuctions.classList.add('active');
        contentProducts.classList.remove('active');
        contentAuctions.classList.add('active');
    }
}

function renderSellerProducts() {
    const tbody = document.getElementById('seller-products-tbody');
    tbody.innerHTML = '';
    
    if (state.products.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--text-muted);">No tienes productos registrados. Crea uno nuevo.</td></tr>`;
        return;
    }
    
    state.products.forEach(p => {
        const catName = state.categories.find(c => c.id === p.categoriaId)?.nombre || `ID: ${p.categoriaId}`;
        const badgeState = p.estado === 'ACTIVO' ? `<span class="badge badge-success">Activo</span>` : `<span class="badge badge-default">Inactivo</span>`;
        
        tbody.innerHTML += `
            <tr>
                <td><strong>${p.id}</strong></td>
                <td>${p.nombre}</td>
                <td>${p.descripcion || '-'}</td>
                <td>${catName}</td>
                <td>${badgeState}</td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick="openEditProductModal(${p.id})">
                        <i class="fa-solid fa-edit"></i>
                    </button>
                </td>
            </tr>
        `;
    });
}

function renderSellerAuctions() {
    const tbody = document.getElementById('seller-auctions-tbody');
    tbody.innerHTML = '';
    
    if (state.auctions.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color: var(--text-muted);">No tienes subastas creadas. Crea una nueva.</td></tr>`;
        return;
    }
    
    state.auctions.forEach(a => {
        const currentVal = a.montoActual || a.precioBase;
        const stateBadge = getAuctionStateBadge(a.estado);
        
        tbody.innerHTML += `
            <tr>
                <td><strong>${a.id}</strong></td>
                <td>${a.titulo}</td>
                <td>ID Producto: ${a.productoId}</td>
                <td>
                    <div style="font-size:0.8rem; color:var(--text-muted);">Base: $${a.precioBase.toLocaleString()}</div>
                    <strong>$${currentVal.toLocaleString()}</strong>
                </td>
                <td>
                    <div style="font-size:0.75rem;">I: ${formatDate(a.fechaInicio)}</div>
                    <div style="font-size:0.75rem; color:var(--text-muted)">C: ${formatDate(a.fechaCierre)}</div>
                </td>
                <td>${stateBadge}</td>
                <td>
                    <div style="display:flex; gap:0.5rem;">
                        <button class="btn btn-secondary btn-sm" onclick="viewAuctionDetail(${a.id})" title="Ver detalles y pujas">
                            <i class="fa-solid fa-eye"></i>
                        </button>
                        ${a.estado === 'BORRADOR' ? `
                            <button class="btn btn-secondary btn-sm" onclick="openEditAuctionModal(${a.id})" title="Editar subasta">
                                <i class="fa-solid fa-edit"></i>
                            </button>
                        ` : ''}
                        ${a.estado === 'ACTIVA' ? `
                            <button class="btn btn-danger btn-sm" onclick="closeAuctionManual(${a.id})" title="Cerrar subasta">
                                <i class="fa-solid fa-lock"></i>
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    });
}

async function loadBuyerPanel() {
    if (!state.user || state.user.rol !== 'ROLE_USER') {
        navigateTo('catalog');
        return;
    }
    
    switchBuyerTab(state.buyerActiveTab);
    
    try {
        
        const wonAuctions = [];
        
        const adjRes = await apiCall('/api/subastas/estado/ADJUDICADA', { method: 'GET' });
        if (adjRes.ok) {
            const adjList = await adjRes.json();
            adjList.forEach(a => {
                if (a.ganadorId === state.user.id) wonAuctions.push(a);
            });
        }
        
        const dispRes = await apiCall('/api/subastas/estado/EN_DISPUTA', { method: 'GET' });
        if (dispRes.ok) {
            const dispList = await dispRes.json();
            dispList.forEach(a => {
                if (a.ganadorId === state.user.id) wonAuctions.push(a);
            });
        }
        
        renderBuyerWon(wonAuctions);

        const activeWinning = [];
        const actRes = await apiCall('/api/subastas/estado/ACTIVA', { method: 'GET' });
        if (actRes.ok) {
            const actList = await actRes.json();
            actList.forEach(a => {
                if (a.ganadorActualId === state.user.id) {
                    activeWinning.push(a);
                }
            });
        }
        renderBuyerActive(activeWinning);
        
    } catch(e) {
        console.error('Error loading buyer panel:', e);
    }
}

function switchBuyerTab(tab) {
    state.buyerActiveTab = tab;
    const tabWon = document.getElementById('tab-buyer-won');
    const tabActive = document.getElementById('tab-buyer-active');
    const contentWon = document.getElementById('buyer-tab-won');
    const contentActive = document.getElementById('buyer-tab-active');
    
    if (tab === 'won') {
        tabWon.classList.add('active');
        tabActive.classList.remove('active');
        contentWon.classList.add('active');
        contentActive.classList.remove('active');
    } else {
        tabWon.classList.remove('active');
        tabActive.classList.add('active');
        contentWon.classList.remove('active');
        contentActive.classList.add('active');
    }
}

function renderBuyerWon(auctions) {
    const tbody = document.getElementById('buyer-won-tbody');
    tbody.innerHTML = '';
    
    if (auctions.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color: var(--text-muted);">No tienes subastas ganadas aún.</td></tr>`;
        return;
    }
    
    auctions.forEach(a => {
        const finalPrice = a.precioFinal || a.montoActual || a.precioBase;
        const stateBadge = getAuctionStateBadge(a.estado);
        
        let actionHtml = `
            <div style="display:flex; gap:0.5rem;">
                <button class="btn btn-secondary btn-sm" onclick="viewAuctionDetail(${a.id})" title="Ver Detalles">
                    <i class="fa-solid fa-eye"></i> Ver
                </button>
        `;
        
        if (a.estado === 'ADJUDICADA') {
            actionHtml += `
                <button class="btn btn-danger btn-sm" onclick="openOpenDisputeModal(${a.id})" title="Abrir Disputa">
                    <i class="fa-solid fa-triangle-exclamation"></i> Reclamar
                </button>
            `;
        }
        
        actionHtml += `</div>`;
        
        tbody.innerHTML += `
            <tr>
                <td><strong>${a.id}</strong></td>
                <td>${a.titulo}</td>
                <td>Vendedor ID: ${a.vendedorId} (${a.vendedorUsername || 'User'})</td>
                <td style="font-weight:700; color:var(--accent)">$${finalPrice.toLocaleString()}</td>
                <td>${formatDate(a.fechaAdjudicacion)}</td>
                <td>${stateBadge}</td>
                <td>${actionHtml}</td>
            </tr>
        `;
    });
}

function renderBuyerActive(auctions) {
    const tbody = document.getElementById('buyer-active-tbody');
    tbody.innerHTML = '';
    
    if (auctions.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color: var(--text-muted);">No lideras ninguna puja en este momento.</td></tr>`;
        return;
    }
    
    auctions.forEach(a => {
        const myBid = a.montoActual;
        const stateBadge = getAuctionStateBadge(a.estado);
        
        tbody.innerHTML += `
            <tr>
                <td><strong>${a.id}</strong></td>
                <td>${a.titulo}</td>
                <td style="font-weight:700; color:var(--success)">$${myBid.toLocaleString()}</td>
                <td>$${a.incrementoMinimoPuja.toLocaleString()}</td>
                <td>${formatDate(a.fechaCierre)}</td>
                <td>${stateBadge}</td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick="viewAuctionDetail(${a.id})" title="Ver Detalles y Pujar">
                        <i class="fa-solid fa-eye"></i> Pujar / Ver
                    </button>
                </td>
            </tr>
        `;
    });
}

async function loadAdminPanel() {
    if (!state.user || state.user.rol !== 'ROLE_ADMIN') {
        navigateTo('catalog');
        return;
    }
    
    switchAdminTab(state.adminActiveTab);
    
    if (state.adminActiveTab === 'users') {
        await loadAdminUsers();
    } else {
        await loadAdminDisputes();
    }
}

function switchAdminTab(tab) {
    state.adminActiveTab = tab;
    
    const tabUsers = document.getElementById('tab-admin-users');
    const tabDisputes = document.getElementById('tab-admin-disputes');
    const contentUsers = document.getElementById('admin-tab-users');
    const contentDisputes = document.getElementById('admin-tab-disputes');
    
    if (tab === 'users') {
        tabUsers.classList.add('active');
        tabDisputes.classList.remove('active');
        contentUsers.classList.add('active');
        contentDisputes.classList.remove('active');
        loadAdminUsers();
    } else {
        tabUsers.classList.remove('active');
        tabDisputes.classList.add('active');
        contentUsers.classList.remove('active');
        contentDisputes.classList.add('active');
        loadAdminDisputes();
    }
}

async function loadAdminUsers() {
    try {
        const response = await apiCall('/api/usuarios', { method: 'GET' });
        if (response.ok) {
            state.allUsers = await response.json();
            renderAdminUsers();
        } else {
            showToast('Error al cargar lista de usuarios.', 'error');
        }
    } catch(e) {
        console.error(e);
    }
}

function renderAdminUsers() {
    const tbody = document.getElementById('admin-users-tbody');
    tbody.innerHTML = '';
    
    state.allUsers.forEach(u => {
        const stateBadge = u.estado === 'ACTIVO' ? `<span class="badge badge-success">Activo</span>` : `<span class="badge badge-danger">Bloqueado</span>`;
        const strikesBadge = u.incidenciasAcumuladas >= 3 ? 
            `<span class="badge badge-danger">${u.incidenciasAcumuladas} strikes (Máximo)</span>` : 
            (u.incidenciasAcumuladas > 0 ? `<span class="badge badge-warning">${u.incidenciasAcumuladas} strikes</span>` : `<span class="badge badge-default">Sin strikes</span>`);
            
        const blockAuditory = u.estado === 'BLOQUEADO' ? `
            <div style="font-size:0.75rem; color:var(--text-muted)">
                Por Admin ID: ${u.bloqueadoPorId || 'Sistema'}
                <div style="font-style:italic;">"${u.motivoBloqueo || 'Sin motivo'}"</div>
            </div>
        ` : '-';
        
        tbody.innerHTML += `
            <tr>
                <td><strong>${u.id}</strong></td>
                <td>${u.username}</td>
                <td>${u.email}</td>
                <td><span class="badge badge-default">${u.rol.replace('ROLE_', '')}</span></td>
                <td>${strikesBadge}</td>
                <td>${stateBadge}</td>
                <td>${blockAuditory}</td>
                <td>
                    ${u.estado === 'ACTIVO' && u.rol !== 'ROLE_ADMIN' ? `
                        <button class="btn btn-danger btn-sm" onclick="openBlockUserModal(${u.id}, '${u.username}')">
                            <i class="fa-solid fa-ban"></i> Bloquear
                        </button>
                    ` : '-'}
                </td>
            </tr>
        `;
    });
}

async function loadAdminDisputes() {
    try {
        const selectedState = document.getElementById('admin-disputes-filter').value;
        let endpoint = `/api/disputas/estado/${selectedState}`;
        if (selectedState === 'RESOLVED_BY_ME') {
            endpoint = `/api/disputas/admin/${state.user.id}`;
        }
        
        const response = await apiCall(endpoint, { method: 'GET' });
        
        if (response.ok) {
            state.disputes = await response.json();
            renderAdminDisputes();
        } else {
            showToast('Error al cargar disputas.', 'error');
        }
    } catch(e) {
        console.error(e);
    }
}

function renderAdminDisputes() {
    const tbody = document.getElementById('admin-disputes-tbody');
    tbody.innerHTML = '';
    
    if (state.disputes.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:var(--text-muted)">No hay disputas en este estado.</td></tr>`;
        return;
    }
    
    state.disputes.forEach(d => {
        let stateBadge = '';
        if (d.estado === 'ABIERTA') {
            stateBadge = `<span class="badge badge-danger">Abierta</span>`;
        } else if (d.estado === 'RESUELTA_FAVOR_USER') {
            stateBadge = `<span class="badge badge-success">Resuelta Comprador</span>`;
        } else {
            stateBadge = `<span class="badge badge-primary">Resuelta Vendedor</span>`;
        }
        
        tbody.innerHTML += `
            <tr>
                <td><strong>${d.id}</strong></td>
                <td><a href="#" onclick="viewAuctionDetail(${d.subastaId})" style="color:var(--accent)">Subasta #${d.subastaId}</a></td>
                <td>Iniciador ID: ${d.iniciadorId}</td>
                <td>${d.motivoApertura}</td>
                <td>${stateBadge}</td>
                <td>${d.adminResolutorId || '-'}</td>
                <td>${d.justificacionResolucion || '-'}</td>
                <td>
                    <div style="display:flex; gap:0.25rem;">
                        <button class="btn btn-secondary btn-sm" onclick="viewDisputeDetail(${d.id})" title="Ver Detalles (TC-23)">
                            <i class="fa-solid fa-eye"></i>
                        </button>
                        ${d.estado === 'ABIERTA' ? `
                            <button class="btn btn-primary btn-sm" onclick="openResolveDisputeModal(${d.id})">
                                Resolver
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    });
}

async function loadNotifications() {
    if (!state.user) return;
    
    try {
        const response = await apiCall(`/api/notificaciones/usuario/${state.user.id}`, { method: 'GET' });
        if (response.ok) {
            state.notifications = await response.json();
            renderNotifications();
            updateNavbar(); 
        }
    } catch(e) {
        console.error(e);
    }
}

function renderNotifications() {
    const tbody = document.getElementById('notifications-tbody');
    const emptyState = document.getElementById('notifications-empty-state');
    
    tbody.innerHTML = '';
    
    if (state.notifications.length === 0) {
        emptyState.style.display = 'block';
        return;
    }
    
    emptyState.style.display = 'none';
    
    state.notifications.forEach(n => {
        let typeBadge = '';
        switch (n.tipo) {
            case 'GANADOR': typeBadge = `<span class="badge badge-success">Adjudicación</span>`; break;
            case 'VENDEDOR': typeBadge = `<span class="badge badge-primary">Venta</span>`; break;
            case 'PUJA_SUPERADA': typeBadge = `<span class="badge badge-warning">Puja Superada</span>`; break;
            default: typeBadge = `<span class="badge badge-default">Sistema</span>`; break;
        }
        
        const readStatus = n.leida ? 
            `<span style="color:var(--text-muted)"><i class="fa-solid fa-envelope-open"></i> Leída</span>` : 
            `<span style="color:var(--accent); font-weight:700;"><i class="fa-solid fa-envelope"></i> Nueva</span>`;
            
        tbody.innerHTML += `
            <tr style="${n.leida ? '' : 'background-color:rgba(0, 242, 254, 0.02)'}">
                <td><span style="font-size:0.8rem; color:var(--text-muted);">${formatDate(n.fechaEnvio)}</span></td>
                <td>${typeBadge}</td>
                <td style="color: ${n.leida ? 'var(--text-default)' : 'var(--text-bright)'}">${n.mensaje}</td>
                <td>${readStatus}</td>
                <td>
                    <div style="display:flex; gap:0.5rem;">
                        ${n.subastaId ? `
                            <button class="btn btn-secondary btn-sm" onclick="viewAuctionDetail(${n.subastaId}); markNotificationAsRead(${n.id});" title="Ir a la subasta">
                                <i class="fa-solid fa-arrow-up-right-from-square"></i>
                            </button>
                        ` : ''}
                        ${!n.leida ? `
                            <button class="btn btn-secondary btn-sm" onclick="markNotificationAsRead(${n.id})" title="Marcar como leída">
                                <i class="fa-solid fa-check"></i>
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    });
}

async function markNotificationAsRead(id) {
    try {
        const response = await apiCall(`/api/notificaciones/${id}/leer`, { method: 'PUT' });
        if (response.ok) {
            
            await loadNotifications();
        }
    } catch(e) {
        console.error(e);
    }
}

function startNotificationsPolling() {
    if (state.notificationsInterval) {
        clearInterval(state.notificationsInterval);
    }
    
    if (state.user) {
        
        state.notificationsInterval = setInterval(() => {
            loadNotifications();
        }, 30000);

        loadNotifications();
    }
}

function openModal(id) {
    document.getElementById(id).classList.add('active');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

function openCreateProductModal() {
    document.getElementById('form-product').reset();
    document.getElementById('product-id').value = '';
    document.getElementById('product-modal-title').textContent = 'Registrar Nuevo Producto';
    openModal('modal-product');
}

function openEditProductModal(id) {
    const prod = state.products.find(p => p.id === id);
    if (!prod) return;
    
    document.getElementById('product-id').value = prod.id;
    document.getElementById('product-name').value = prod.nombre;
    document.getElementById('product-description').value = prod.descripcion;
    document.getElementById('product-category').value = prod.categoriaId;
    document.getElementById('product-state').value = prod.estado;
    document.getElementById('product-modal-title').textContent = 'Editar Producto';
    
    openModal('modal-product');
}

async function handleProductSubmit(event) {
    event.preventDefault();
    const id = document.getElementById('product-id').value;
    const nombre = document.getElementById('product-name').value.trim();
    const descripcion = document.getElementById('product-description').value.trim();
    const categoriaId = parseInt(document.getElementById('product-category').value);
    const estado = document.getElementById('product-state').value;
    
    const payload = {
        categoriaId,
        vendedorId: state.user.id,
        nombre,
        descripcion,
        estado
    };
    
    try {
        let response;
        if (id) {
            response = await apiCall(`/api/productos/${id}`, {
                method: 'PUT',
                body: payload
            });
        } else {
            response = await apiCall('/api/productos', {
                method: 'POST',
                body: payload
            });
        }
        
        if (response.ok) {
            showToast(id ? 'Producto actualizado con éxito.' : 'Producto registrado con éxito.', 'success');
            closeModal('modal-product');
            await loadSellerPanel();
        } else {
            const errMsg = await getErrorMessage(response, 'Error al guardar el producto.');
            showToast(errMsg, 'warning');
        }
    } catch(e) {
        console.error(e);
    }
}

async function openCreateAuctionModal() {
    document.getElementById('form-auction').reset();
    document.getElementById('auction-id').value = '';
    document.getElementById('auction-modal-title').textContent = 'Programar Nueva Subasta';

    const now = new Date();
    const tomorrow = new Date(now.getTime() + (24 * 60 * 60 * 1000));
    
    document.getElementById('auction-date-start').value = formatDateTimeLocal(now);
    document.getElementById('auction-date-end').value = formatDateTimeLocal(tomorrow);

    const productSelect = document.getElementById('auction-product');
    productSelect.innerHTML = '';
    
    const activeProducts = state.products.filter(p => p.estado === 'ACTIVO');
    if (activeProducts.length === 0) {
        showToast('Debe registrar al menos un producto ACTIVO para crear una subasta.', 'warning');
        openCreateProductModal();
        return;
    }
    
    activeProducts.forEach(p => {
        productSelect.innerHTML += `<option value="${p.id}">${p.nombre} (ID: ${p.id})</option>`;
    });
    
    openModal('modal-auction');
}

async function openEditAuctionModal(id) {
    const auc = state.auctions.find(a => a.id === id);
    if (!auc) return;
    
    document.getElementById('auction-id').value = auc.id;
    document.getElementById('auction-title').value = auc.titulo;
    document.getElementById('auction-description').value = auc.descripcion;
    document.getElementById('auction-price-base').value = auc.precioBase;
    document.getElementById('auction-price-min-increment').value = auc.incrementoMinimoPuja;
    document.getElementById('auction-date-start').value = formatDateTimeLocal(new Date(auc.fechaInicio));
    document.getElementById('auction-date-end').value = formatDateTimeLocal(new Date(auc.fechaCierre));
    document.getElementById('auction-state').value = auc.estado;
    
    const productSelect = document.getElementById('auction-product');
    productSelect.innerHTML = `<option value="${auc.productoId}">Preservar Producto Actual (ID: ${auc.productoId})</option>`;
    
    document.getElementById('auction-modal-title').textContent = 'Editar Subasta (Borrador)';
    openModal('modal-auction');
}

async function handleAuctionSubmit(event) {
    event.preventDefault();
    const id = document.getElementById('auction-id').value;
    const titulo = document.getElementById('auction-title').value.trim();
    const descripcion = document.getElementById('auction-description').value.trim();
    const productoId = parseInt(document.getElementById('auction-product').value);
    const estado = document.getElementById('auction-state').value;
    const precioBase = parseFloat(document.getElementById('auction-price-base').value);
    const incrementoMinimoPuja = parseFloat(document.getElementById('auction-price-min-increment').value);
    
    const fechaInicioRaw = document.getElementById('auction-date-start').value;
    const fechaCierreRaw = document.getElementById('auction-date-end').value;

    let startDate = new Date(fechaInicioRaw);
    let endDate = new Date(fechaCierreRaw);
    
    const now = new Date();

    if (startDate <= now) {
        startDate = new Date(now.getTime() + 10000); 
    }
    
    const fechaInicio = formatDateTimeBackend(startDate);
    const fechaCierre = formatDateTimeBackend(endDate);
    
    const payload = {
        vendedorId: state.user.id,
        productoId,
        precioBase,
        incrementoMinimoPuja,
        titulo,
        descripcion,
        fechaInicio,
        fechaCierre,
        estado
    };
    
    try {
        let response;
        if (id) {
            response = await apiCall(`/api/subastas/${id}`, {
                method: 'PUT',
                body: payload
            });
        } else {
            response = await apiCall('/api/subastas', {
                method: 'POST',
                body: payload
            });
        }
        
        if (response.ok) {
            showToast(id ? 'Subasta actualizada con éxito.' : 'Subasta creada con éxito.', 'success');
            closeModal('modal-auction');
            await loadSellerPanel();
        } else {
            const errMsg = await getErrorMessage(response, 'Error al guardar la subasta.');
            showToast(errMsg, 'warning');
        }
    } catch(e) {
        console.error(e);
    }
}

function openBlockUserModal(id, username) {
    document.getElementById('block-user-id').value = id;
    document.getElementById('block-user-name-display').textContent = username;
    document.getElementById('block-user-reason').value = '';
    openModal('modal-block-user');
}

async function handleBlockUserSubmit(event) {
    event.preventDefault();
    const id = document.getElementById('block-user-id').value;
    const motivo = document.getElementById('block-user-reason').value.trim();
    
    try {
        
        const response = await apiCall(`/api/usuarios/${id}/bloquear?motivo=${encodeURIComponent(motivo)}`, {
            method: 'PUT'
        });
        
        if (response.ok) {
            showToast('El usuario ha sido bloqueado correctamente.', 'success');
            closeModal('modal-block-user');
            await loadAdminUsers();
        } else {
            showToast('Error al bloquear usuario.', 'error');
        }
    } catch(e) {
        console.error(e);
    }
}

function openOpenDisputeModal(auctionId) {
    document.getElementById('open-dispute-auction-id').value = auctionId;
    document.getElementById('open-dispute-reason').value = '';
    openModal('modal-open-dispute');
}

async function handleOpenDisputeSubmit(event) {
    event.preventDefault();
    const subastaId = parseInt(document.getElementById('open-dispute-auction-id').value);
    const motivoApertura = document.getElementById('open-dispute-reason').value.trim();
    
    try {
        const response = await apiCall('/api/disputas', {
            method: 'POST',
            body: {
                subastaId,
                iniciadorId: state.user.id,
                motivoApertura
            }
        });
        
        if (response.status === 201) {
            showToast('Disputa abierta con éxito. Bloqueando subasta.', 'success');
            closeModal('modal-open-dispute');
            if (state.activeView === 'detail') {
                await loadAuctionDetailData();
            }
        } else {
            const errMsg = await getErrorMessage(response, 'Error al abrir la disputa.');
            showToast(errMsg, 'warning');
        }
    } catch(e) {
        console.error(e);
    }
}

async function viewDisputeDetail(id) {
    try {
        const response = await apiCall(`/api/disputas/${id}`, { method: 'GET' });
        if (response.ok) {
            const d = await response.json();
            document.getElementById('view-dispute-id').textContent = d.id;
            document.getElementById('view-dispute-subasta').textContent = `ID Subasta: ${d.subastaId}`;
            document.getElementById('view-dispute-iniciador').textContent = `ID Usuario: ${d.iniciadorId}`;
            document.getElementById('view-dispute-motivo').textContent = d.motivoApertura;
            
            const badgeContainer = document.getElementById('view-dispute-estado');
            badgeContainer.innerHTML = '';
            let stateBadge = '';
            if (d.estado === 'ABIERTA') {
                stateBadge = '<span class="badge badge-danger">Abierta</span>';
            } else if (d.estado === 'RESUELTA_FAVOR_USER') {
                stateBadge = '<span class="badge badge-success">Resuelta Favor Comprador</span>';
            } else {
                stateBadge = '<span class="badge badge-primary">Resuelta Favor Vendedor</span>';
            }
            badgeContainer.innerHTML = stateBadge;
            
            document.getElementById('view-dispute-admin').textContent = d.adminResolutorId ? `ID Admin: ${d.adminResolutorId}` : 'No resuelta aún';
            
            const justGroup = document.getElementById('view-dispute-justificacion-group');
            if (d.estado !== 'ABIERTA') {
                justGroup.style.display = 'block';
                document.getElementById('view-dispute-justificacion').textContent = d.justificacionResolucion || 'Sin detalles.';
            } else {
                justGroup.style.display = 'none';
            }
            
            openModal('modal-view-dispute');
        } else {
            showToast('No se pudieron obtener detalles de la disputa.', 'error');
        }
    } catch(e) {
        console.error(e);
    }
}

function openResolveDisputeModal(disputeId) {
    document.getElementById('resolve-dispute-id').value = disputeId;
    document.getElementById('resolve-dispute-id-display').textContent = `#${disputeId}`;
    document.getElementById('resolve-dispute-justification').value = '';
    openModal('modal-resolve-dispute');
}

async function handleResolveDisputeSubmit(event) {
    event.preventDefault();
    const id = document.getElementById('resolve-dispute-id').value;
    const nuevoEstado = document.getElementById('resolve-dispute-choice').value;
    const justificacion = document.getElementById('resolve-dispute-justification').value.trim();
    
    try {
        
        const endpoint = `/api/disputas/${id}/resolver?adminId=${state.user.id}&nuevoEstado=${nuevoEstado}&justificacion=${encodeURIComponent(justificacion)}`;
        
        const response = await apiCall(endpoint, {
            method: 'POST'
        });
        
        if (response.ok) {
            showToast('Disputa resuelta y strike impuesto con éxito.', 'success');
            closeModal('modal-resolve-dispute');
            await loadAdminDisputes();
        } else {
            showToast('Error al resolver la disputa.', 'error');
        }
    } catch(e) {
        console.error(e);
    }
}

function toggleConsole() {
    const apiConsole = document.getElementById('api-console');
    const icon = document.getElementById('console-toggle-icon').querySelector('i');
    
    if (apiConsole.classList.contains('collapsed')) {
        apiConsole.classList.remove('collapsed');
        icon.classList.replace('fa-chevron-up', 'fa-chevron-down');
    } else {
        apiConsole.classList.add('collapsed');
        icon.classList.replace('fa-chevron-down', 'fa-chevron-up');
    }
}

function logConsole(type, method, url, status, responseJson, requestJson = null, duration = null) {
    const logsBody = document.getElementById('console-logs-body');
    const now = new Date();
    const timeStr = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`;
    
    let lineClass = 'success';
    if (type === 'error') lineClass = 'error';
    if (type === 'info') lineClass = 'info';
    
    let statusClass = status >= 200 && status < 300 ? 'success' : 'error';
    if (status === 'FETCH_FAILED' || status === 'JWT') statusClass = 'error';

    let logHtml = `
        <div class="console-line">
            <span class="console-time">[${timeStr}]</span>
            <span class="console-method ${method}">${method}</span>
            <span class="console-url">${url}</span>
            ${status ? `<span class="console-status ${statusClass}">${status} ${duration ? `(${duration}ms)` : ''}</span>` : ''}
    `;
    
    if (requestJson) {
        logHtml += `
            <div style="font-size: 0.75rem; color: #aaa; margin-top: 0.2rem;">Request Payload:</div>
            <pre class="console-json">${JSON.stringify(requestJson, null, 2)}</pre>
        `;
    }
    
    if (responseJson) {
        logHtml += `
            <div style="font-size: 0.75rem; color: #aaa; margin-top: 0.2rem;">Response Payload:</div>
            <pre class="console-json">${JSON.stringify(responseJson, null, 2)}</pre>
        `;
    }
    
    logHtml += `</div>`;

    if (logsBody.innerHTML.includes('Inicializando consola')) {
        logsBody.innerHTML = '';
    }

    logsBody.innerHTML += logHtml;
    logsBody.scrollTop = logsBody.scrollHeight;

    if (type === 'error') {
        const apiConsole = document.getElementById('api-console');
        if (apiConsole.classList.contains('collapsed')) {
            toggleConsole();
        }
    }
}

function showToast(message, type = 'info') {
    const wrapper = document.getElementById('toast-wrapper');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = '<i class="fa-solid fa-circle-info"></i>';
    if (type === 'success') icon = '<i class="fa-solid fa-circle-check"></i>';
    if (type === 'error') icon = '<i class="fa-solid fa-circle-exclamation"></i>';
    if (type === 'warning') icon = '<i class="fa-solid fa-triangle-exclamation"></i>';
    
    toast.innerHTML = `
        ${icon}
        <span>${message}</span>
    `;
    
    wrapper.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideInRight 0.3s reverse forwards';
        setTimeout(() => {
            if (toast.parentNode === wrapper) {
                wrapper.removeChild(toast);
            }
        }, 300);
    }, 4000);
}

async function getErrorMessage(response, defaultMsg) {
    try {
        const cloned = response.clone();
        const contentType = cloned.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const errBody = await cloned.json();
            if (errBody.invalid_params) {
                const details = Object.entries(errBody.invalid_params)
                    .map(([field, msg]) => `${field}: ${msg}`)
                    .join(' | ');
                return `${errBody.detail || 'Error de validación'}: ${details}`;
            }
            return errBody.detail || errBody.message || defaultMsg;
        } else {
            const text = await cloned.text();
            return text || defaultMsg;
        }
    } catch (e) {
        return defaultMsg;
    }
}

function formatDate(dateString) {
    if (!dateString) return '-';
    try {
        const d = new Date(dateString);
        if (isNaN(d.getTime())) return dateString;
        return d.toLocaleString('es-AR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch(e) {
        return dateString;
    }
}

function formatDateTimeLocal(dateObj) {
    const tzOffset = dateObj.getTimezoneOffset() * 60000; 
    const localISOTime = (new Date(dateObj - tzOffset)).toISOString().slice(0, 16);
    return localISOTime;
}

function formatDateTimeBackend(dateObj) {
    const pad = (n) => String(n).padStart(2, '0');
    return `${dateObj.getFullYear()}-${pad(dateObj.getMonth()+1)}-${pad(dateObj.getDate())}T${pad(dateObj.getHours())}:${pad(dateObj.getMinutes())}:00`;
}
