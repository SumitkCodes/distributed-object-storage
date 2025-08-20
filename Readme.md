# 🚀 Mini-S3: Distributed Object Storage System

A production-ready, high-performance distributed file storage system built with Java 17, Spring Boot 3, and PostgreSQL. This project implements core S3-like functionality including object storage, replication, versioning, and fault tolerance.

## ✨ What Does This Project Do?

Mini-S3 is like having your own private Amazon S3 storage system running on your computer. It allows you to:

- **Store files** in organized buckets (like folders)
- **Automatically backup files** across multiple storage locations
- **Access files** from anywhere on your network
- **Manage file versions** (keep multiple versions of the same file)
- **Scale storage** by adding more storage nodes

Think of it as a smart file cabinet that automatically makes copies of your important files and keeps them safe even if one storage location fails.

## 🎯 **Want to See It in Action? Try Our Demo!**

The best way to understand Mini-S3 is to see it working! We've created a comprehensive demo script that showcases every feature:

```bash
# After setting up the system, run:
chmod +x demo.sh
./demo.sh
```

**🎬 What You'll See:**
- 🚀 **Complete system startup** with health checks
- 🔒 **Security features** blocking invalid inputs
- 📁 **File operations** with replication across nodes
- 🔄 **Versioning system** managing multiple file versions
- ⚡ **Performance monitoring** with real-time metrics
- 🎯 **Production readiness** verification

**💡 Perfect for:**
- 📸 **GitHub screenshots** showing your system working
- 🎓 **Learning** how distributed storage works
- 🔍 **Testing** that everything functions correctly
- 🚀 **Impressing** others with your technical skills

*The demo takes about 2-3 minutes and provides a complete tour of the system!*

## 🔒 Security Features (Why This is Safe to Use)

### Input Validation & Sanitization
- **Comprehensive validation** for all API endpoints
- **Bucket name validation** (3-63 chars, lowercase, numbers, dots, hyphens)
- **Object key validation** (max 1024 characters)
- **Node name validation** (2-50 chars, alphanumeric, dots, hyphens)
- **URL validation** for storage node base URLs

### File Security
- **File size limits** (10MB maximum) - prevents abuse
- **Allowed file extensions** validation - blocks dangerous file types
- **Enhanced path traversal protection** - prevents hackers from accessing files outside storage
- **File content validation** and checksums - ensures file integrity

### Network Security
- **All services bound to localhost only** - not accessible from internet
- **Connection timeouts** and header size limits - prevents attacks
- **Restricted management endpoints** (health, info only)
- **PostgreSQL authentication enforcement** (md5)

## ⚡ Performance Features

### Database Performance
- **HikariCP connection pooling** (10 max, 5 min idle)
- **Hibernate batching** (batch size 20)
- **Optimized fetch types** (LAZY loading for relationships)
- **Connection timeout** and idle timeout configurations

### File Operations
- **Streaming file uploads/downloads** - handles large files efficiently
- **Efficient file size parsing** with unit support (KB, MB, GB)
- **Concurrent replication** with proper error handling
- **Optimized storage path generation**

## 🛠️ Tech Stack

- **Backend**: Java 17, Spring Boot 3.3.2
- **Database**: PostgreSQL 15 with HikariCP connection pooling
- **Containerization**: Docker & Docker Compose
- **Build Tool**: Maven 3.13.0
- **Architecture**: Microservices (API Gateway + Storage Nodes)

## 📋 Prerequisites

Before you begin, ensure you have the following installed on your system:

- **Java 17** or higher
- **Maven 3.6+**
- **Docker Desktop** with Docker Compose
- **Git** for version control

### Quick Installation Commands

**macOS (using Homebrew):**
```bash
brew install openjdk@17
brew install maven
brew install --cask docker
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-17-jdk maven docker.io docker-compose
```

**Windows:**
Download and install from the official websites:
- [Java 17](https://adoptium.net/)
- [Maven](https://maven.apache.org/download.cgi)
- [Docker Desktop](https://www.docker.com/products/docker-desktop)

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone <your-repo-url>
cd mini-s3
```

### 2. Build the Project
```bash
mvn clean package -DskipTests
```

This command will:
- Compile all Java source code
- Package the applications into executable JAR files
- Skip running tests (use `mvn clean package` if you want to run tests)

### 3. Start the System
```bash
docker compose up -d
```

The `-d` flag runs the services in the background.

### 4. Wait for Services to Start
```bash
docker compose ps
```

Wait until all services show "Up" status. The first startup may take a few minutes as Docker downloads the base images.

### 5. Verify System Status
```bash
# Check all services are running
docker compose ps

# Test storage node health
curl http://localhost:9091/health

# Test API status
curl http://localhost:8080/buckets
```

### 6. 🎯 **Run the Complete Demo (Optional but Recommended!)**
Want to see everything working together? Run our comprehensive demo:

```bash
chmod +x demo.sh
./demo.sh
```

This will showcase all features: security, replication, versioning, monitoring, and more!

## 📖 How to Use (Step by Step)

### Step 1: Create a Bucket
A bucket is like a folder where you store your files.

```bash
curl -X POST "http://localhost:8080/buckets?name=mybucket"
```

### Step 2: Register Storage Nodes
Storage nodes are the places where your files will be stored. You need to register them first.

```bash
# Register node1
curl -X POST "http://localhost:8080/nodes/register" \
  -H "Content-Type: application/json" \
  -d '{"name":"node1","baseUrl":"http://node1:9091"}'

# Register node2
curl -X POST "http://localhost:8080/nodes/register" \
  -H "Content-Type: application/json" \
  -d '{"name":"node2","baseUrl":"http://node2:9092"}'

# Register node3
curl -X POST "http://localhost:8080/nodes/register" \
  -H "Content-Type: application/json" \
  -d '{"name":"node3","baseUrl":"http://node3:9093"}'
```

### Step 3: Upload a File
Now you can upload files to your bucket.

```bash
curl -X POST "http://localhost:8080/objects/mybucket/myfile.txt" \
  -F "file=@myfile.txt"
```

### Step 4: Download a File
Download files from your bucket.

```bash
curl -s "http://localhost:8080/objects/mybucket/myfile.txt" -o downloaded-file.txt
```

### Step 5: List Objects in Bucket
See what files are in your bucket.

```bash
curl "http://localhost:8080/objects/mybucket"
```

### 🎯 **Pro Tip: Run the Complete Demo!**

Want to see all these features working together? Instead of testing manually, run our comprehensive demo:

```bash
chmod +x demo.sh
./demo.sh
```

This will automatically test everything: bucket creation, node registration, file uploads, downloads, versioning, and more - all with beautiful output and real-time monitoring!

## 🏗️ System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │  Storage Node 1 │    │  Storage Node 2 │
│   (Port 8080)   │◄──►│   (Port 9091)   │    │   (Port 9092)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌─────────────────┐              │
         └──────────────►│  Storage Node 3 │◄─────────────┘
                        │   (Port 9093)   │
                        └─────────────────┘
                                 │
                        ┌─────────────────┐
                        │   PostgreSQL    │
                        │   (Port 5432)   │
                        └─────────────────┘
```

**How it works:**
1. **API Gateway** (Port 8080): This is the main entry point where you send requests
2. **Storage Nodes** (Ports 9091, 9092, 9093): These are where your files are actually stored
3. **PostgreSQL** (Port 5432): This keeps track of all your files, buckets, and storage locations

## 🔧 Configuration

### Environment Variables
Create a `.env` file based on `.env.template`:

```bash
# Database Configuration
POSTGRES_DB=minis3
POSTGRES_USER=your_username
POSTGRES_PASSWORD=your_secure_password

# Storage Configuration
STORAGE_MAX_FILE_SIZE=10MB
STORAGE_ALLOWED_EXTENSIONS=txt,pdf,doc,docx,jpg,jpeg,png,gif,zip,rar,bin

# API Configuration
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
```

### Docker Compose Services
- **API Gateway**: Spring Boot application on port 8080
- **Storage Nodes**: 3 storage nodes on ports 9091, 9092, 9093
- **Database**: PostgreSQL 15 on port 5432
- **Volumes**: Persistent storage for each node

## 📊 API Endpoints

### Bucket Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/buckets?name={name}` | Create a new bucket |
| `GET` | `/buckets` | List all buckets |
| `GET` | `/buckets/{name}` | Get bucket details |
| `DELETE` | `/buckets/{name}` | Delete a bucket |

### Object Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/objects/{bucket}/{key}` | Upload a file |
| `GET` | `/objects/{bucket}/{key}` | Download a file |
| `GET` | `/objects/{bucket}` | List objects in bucket |
| `DELETE` | `/objects/{bucket}/{key}/versions/{version}` | Delete specific version |

### Node Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/nodes/register` | Register a storage node |
| `GET` | `/nodes` | List all nodes |
| `GET` | `/nodes/{name}` | Get node details |
| `PUT` | `/nodes/{name}/status` | Update node status |

### Health & Monitoring
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | API health check |
| `GET` | `{node}:{port}/health` | Storage node health |

## 🔍 Monitoring & Debugging

### Health Checks
```bash
# API Health
curl http://localhost:8080/health

# Storage Node Health
curl http://localhost:9091/health
curl http://localhost:9092/health
curl http://localhost:9093/health
```

### Logs
```bash
# View all service logs
docker compose logs

# View specific service logs
docker compose logs api
docker compose logs node1
docker compose logs db
```

### System Status
```bash
# Check service status
docker compose ps

# Check resource usage
docker stats
```

## 🧪 Testing

### 🚀 **Quick Demo: See All Features in Action!**

Want to see the Mini-S3 system in action? Run our comprehensive demo script to witness all features working together:

```bash
# Make the demo script executable
chmod +x demo.sh

# Run the complete feature demonstration
./demo.sh
```

**What the demo shows you:**
- 🎯 **System Startup & Health Check** - All services running perfectly
- 🔒 **Security Features** - Input validation and protection in action
- 🏗️ **Core Functionality** - Storage nodes, buckets, and infrastructure
- 📁 **File Operations** - Upload, download, replication across nodes
- 🔄 **Versioning & Metadata** - Multiple file versions and organization
- 📥 **Download & Verification** - File integrity and checksum validation
- ⚡ **Performance & Monitoring** - Real-time system metrics and health
- 🔧 **Advanced Features** - Node management and bucket operations
- 🎯 **Final Status** - Complete system verification and production readiness

**Demo Output Features:**
- 🌈 **Color-coded results** (green for success, red for errors)
- 📊 **Real-time system monitoring** with Docker stats
- ✅ **Comprehensive testing** of all endpoints and features
- 🔍 **Security validation** showing protection against bad inputs
- 📈 **Performance metrics** displaying resource usage
- 🎉 **Professional presentation** ready for screenshots

**Perfect for:**
- 📸 **GitHub screenshots** showing your system working
- 🎓 **Learning** how all components work together
- 🔍 **Testing** that everything is functioning correctly
- 🚀 **Demonstrating** the system's capabilities to others

### Manual Testing
```bash
# 1. Create bucket
curl -X POST "http://localhost:8080/buckets?name=testbucket"

# 2. Register nodes
curl -X POST "http://localhost:8080/nodes/register" \
  -H "Content-Type: application/json" \
  -d '{"name":"node1","baseUrl":"http://node1:9091"}'

# 3. Upload file
echo "Hello, World!" > test.txt
curl -X POST "http://localhost:8080/objects/testbucket/test.txt" \
  -F "file=@test.txt"

# 4. Download file
curl -s "http://localhost:8080/objects/testbucket/test.txt" -o downloaded.txt
cat downloaded.txt
```

### Automated Testing
```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run with coverage
mvn jacoco:report
```

## 🚀 Deployment

### Production Deployment
1. **Environment Setup**
   ```bash
   cp .env.template .env
   # Edit .env with production values
   ```

2. **Security Configuration**
   - Change default passwords
   - Configure firewall rules
   - Enable SSL/TLS
   - Set up monitoring

3. **Scaling**
   - Add more storage nodes
   - Configure load balancing
   - Set up database clustering

### Cloud Deployment
- **AWS**: Use ECS/EKS with RDS
- **GCP**: Use GKE with Cloud SQL
- **Azure**: Use AKS with Azure Database

## 🐛 Troubleshooting

### Common Issues

#### Service Won't Start
```bash
# Check logs
docker compose logs <service-name>

# Check port conflicts
netstat -tulpn | grep :8080
```

#### File Upload Fails
```bash
# Check storage node health
curl http://localhost:9091/health

# Check file size limits
# Check allowed file extensions
```

#### Database Connection Issues
```bash
# Check database logs
docker compose logs db

# Verify environment variables
docker compose exec api env | grep SPRING_DATASOURCE
```

### Performance Issues
- Check connection pool settings
- Monitor database query performance
- Verify storage node disk space
- Check network latency between services

## 🧹 Cleanup

### Stop Services
```bash
docker compose down
```

### Remove All Data (⚠️ Destructive)
```bash
docker compose down -v
rm -rf data/
```

### Rebuild from Scratch
```bash
docker compose down
docker system prune -f
mvn clean package -DskipTests
docker compose up --build -d
```

## 🤝 Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

### Code Standards
- Follow Java coding conventions
- Add comprehensive JavaDoc
- Include unit tests
- Update documentation



## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- PostgreSQL community for the robust database
- Docker team for containerization tools


---

⭐ **If you find this project useful, consider giving it a star or contributing to its development!**

**Happy coding! 🚀**
