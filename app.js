const express = require('express');
const multer = require('multer');
const cors = require('cors');
const path = require('path');
const logger = require('./logger');
const { errorHandler, AppError } = require('./errorHandler');

const app = express();
const port = 8000;  // Using port 8000 as specified in the requirements

// Configure multer for file upload
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, 'uploads/');
    },
    filename: function (req, file, cb) {
        cb(null, Date.now() + '-' + file.originalname);
    }
});

const upload = multer({ 
    storage: storage,
    limits: {
        fileSize: 100 * 1024 * 1024 // 100MB limit
    }
});

// Middleware
app.use(cors());
app.use(express.json());

// Create uploads directory if it doesn't exist
const fs = require('fs');
if (!fs.existsSync('uploads')) {
    fs.mkdirSync('uploads');
}

// Logging middleware
app.use((req, res, next) => {
    logger.info({
        method: req.method,
        path: req.path,
        ip: req.ip
    });
    next();
});

// Status endpoint for discovery
app.get('/status', (req, res) => {
    res.json({
        status: 'ok',
        serverName: require('os').hostname(),
        timestamp: new Date().toISOString()
    });
});

// File upload endpoint
app.post('/upload', upload.single('file'), (req, res, next) => {
    try {
        if (!req.file) {
            throw new AppError(400, 'No file uploaded');
        }

        logger.info({
            message: 'File uploaded successfully',
            filename: req.file.originalname,
            size: req.file.size
        });

        res.json({
            status: 'success',
            message: 'File uploaded successfully',
            file: {
                originalName: req.file.originalname,
                size: req.file.size,
                path: req.file.path
            }
        });
    } catch (error) {
        next(error);
    }
});

// Error handling
app.use(errorHandler);

// Start server
app.listen(port, '0.0.0.0', () => {
    logger.info(`Server running at http://0.0.0.0:${port}`);
    logger.info('Server is ready to accept file transfers from Android clients');
});