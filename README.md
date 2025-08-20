# Mini-S3: Distributed File Storage System

A lightweight, production-ready implementation of a distributed file storage system inspired by Amazon S3. This project demonstrates core concepts of distributed systems including replication, fault tolerance, and consistent hashing while providing a simple REST API for file operations.

Perfect for learning distributed systems concepts, building internal file storage solutions, or as a foundation for larger storage infrastructure projects.

## 🚀 Tech Stack

- **Backend**: Java 17 with Spring Boot 3.3.2
- **Database**: PostgreSQL 15 for metadata storage
- **Storage**: Local filesystem-based distributed storage nodes
- **Containerization**: Docker & Docker Compose
- **Build Tool**: Maven 3.9+
- **Architecture**: Multi-module microservices design

## ✨ Features

- **Bucket Management**: Create, list, and manage storage buckets
- **Object Operations**: Upload, download, list, and delete files
- **Automatic Replication**: Files are automatically replicated across multiple storage nodes (configurable replication factor)
- **Versioning**: Support for multiple versions of the same file with soft delete
- **Metadata Tracking**: File size, creation time, checksums, and storage locations
- **Fault Tolerance**: System continues operating even if storage nodes fail
- **Consistent Hashing**: Intelligent file placement across storage nodes
- **Health Monitoring**: Built-in health checks for all services

## 📋 Prerequisites

Before you begin, ensure you have the following installed on your system:

- **Java 17** (OpenJDK or Oracle JDK)
- **Maven 3.9+** (for building the project)
- **Docker Desktop** (for running the services)
- **Git** (for cloning the repository)

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

## 🛠️ Installation & Setup

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

### 3. Start the Services
```bash
docker compose up --build -d
```

The `--build` flag ensures Docker rebuilds the images with your latest code, and `-d` runs the services in the background.

### 4. Wait for Services to Start
```bash
docker compose ps
```

Wait until all services show "Up" status. The first startup may take a few minutes as Docker downloads the base images.

## 🎯 How to Use

### Register Storage Nodes
First, register your storage nodes with the system:

```bash
# Register node1
curl -X POST http://localhost:8080/nodes/register \
  -H "Content-Type: application/json" \
  -d '{"name":"node1","baseUrl":"http://node1:9091"}'

# Register node2
curl -X POST http://localhost:8080/nodes/register \
  -H "Content-Type: application/json" \
  -d '{"name":"node2","baseUrl":"http://node2:9092"}'

# Register node3
curl -X POST http://localhost:8080/nodes/register \
  -H "Content-Type: application/json" \
  -d '{"name":"node3","baseUrl":"http://node3:9093"}'
```

### Create a Bucket
```bash
curl -X POST "http://localhost:8080/buckets?name=mybucket"
```

### Upload a File
```bash
curl -X POST "http://localhost:8080/objects/mybucket/myfile.txt" \
  -F "file=@/path/to/your/local/file.txt"
```

### Download a File
```bash
curl -X GET "http://localhost:8080/objects/mybucket/myfile.txt" \
  -o downloaded-file.txt
```

### List Objects in a Bucket
```bash
curl -X GET "http://localhost:8080/objects/mybucket"
```

### Upload a New Version
```bash
curl -X POST "http://localhost:8080/objects/mybucket/myfile.txt" \
  -F "file=@/path/to/new/version.txt"
```

### Download a Specific Version
```bash
curl -X GET "http://localhost:8080/objects/mybucket/myfile.txt?version=1" \
  -o version1.txt
```

### Delete a Version
```bash
curl -X DELETE "http://localhost:8080/objects/mybucket/myfile.txt?version=1"
```

## 📁 Where Files Are Stored

Files are stored locally on your machine in the following structure:

```
mini-s3/
├── data/
│   ├── node1/          # Storage node 1 files
│   ├── node2/          # Storage node 2 files
│   └── node3/          # Storage node 3 files
```

Each storage node maintains its own copy of files based on the replication strategy. The system automatically distributes files across nodes using consistent hashing, ensuring even load distribution and fault tolerance.

**Note**: The actual file paths within each node folder are determined by the system's internal routing logic and may not directly correspond to your bucket/object names for security reasons.

## 🔌 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/buckets` | GET | List all buckets |
| `/buckets` | POST | Create a new bucket |
| `/objects/{bucket}` | GET | List objects in a bucket |
| `/objects/{bucket}/{key}` | POST | Upload a file |
| `/objects/{bucket}/{key}` | GET | Download a file |
| `/objects/{bucket}/{key}` | DELETE | Delete a specific version |
| `/nodes/register` | POST | Register a storage node |
| `/nodes` | GET | List all storage nodes |

### Query Parameters

- **Bucket creation**: `?name=bucketname`
- **File upload**: Use `multipart/form-data` with `file` field
- **File download**: `?version=X` for specific versions (optional)
- **File deletion**: `?version=X` (required)

## 🚀 Example Workflow

Here's a complete example that demonstrates the basic workflow:

```bash
# 1. Create a test file
echo "Hello, Mini-S3!" > test-file.txt

# 2. Create a bucket
curl -X POST "http://localhost:8080/buckets?name=testbucket"

# 3. Upload the file
curl -X POST "http://localhost:8080/objects/testbucket/hello.txt" \
  -F "file=@test-file.txt"

# 4. List objects in the bucket
curl -X GET "http://localhost:8080/objects/testbucket"

# 5. Download the file
curl -X GET "http://localhost:8080/objects/testbucket/hello.txt" \
  -o downloaded-hello.txt

# 6. Verify the content
cat downloaded-hello.txt
```

## 🔍 Monitoring & Health Checks

### Check Service Status
```bash
docker compose ps
```

### View Service Logs
```bash
# All services
docker compose logs

# Specific service
docker compose logs api
docker compose logs node1
```

### Health Endpoints
```bash
# API health (check if running)
curl http://localhost:8080/buckets

# Storage node health
curl http://localhost:9091/health
curl http://localhost:9092/health
curl http://localhost:9093/health
```

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

## 🐛 Troubleshooting

### Common Issues

**"Connection refused" errors:**
- Ensure Docker Desktop is running
- Wait for services to fully start up
- Check `docker compose ps` for service status

**Build failures:**
- Verify Java 17 is installed: `java -version`
- Verify Maven is installed: `mvn -version`
- Clean and rebuild: `mvn clean package -DskipTests`

**File upload issues:**
- Check file size (current limit is ~1MB)
- Ensure the bucket exists before uploading
- Verify storage nodes are registered and healthy

**Database connection issues:**
- Wait for PostgreSQL to fully start (check health status)
- Verify no other PostgreSQL instance is running on port 5432

### Getting Help

1. Check the service logs: `docker compose logs [service-name]`
2. Verify all prerequisites are installed correctly
3. Ensure Docker has sufficient resources allocated
4. Check that ports 8080, 9091, 9092, 9093, and 5432 are available

## 🔮 Future Improvements

This MVP provides a solid foundation for a production storage system. Future enhancements could include:

- **Authentication & Authorization**: User management and access control
- **File Compression**: Automatic compression for storage efficiency
- **Encryption**: Client-side and server-side encryption
- **Web UI**: Browser-based file management interface
- **Cloud Deployment**: Kubernetes manifests and cloud provider integration
- **Monitoring & Metrics**: Prometheus integration and Grafana dashboards
- **Backup & Recovery**: Automated backup strategies
- **Multi-region Support**: Geographic distribution of storage nodes
- **CDN Integration**: Content delivery network for global access
- **API Rate Limiting**: Protection against abuse

## 📚 Learning Resources

This project demonstrates several important distributed systems concepts:

- **Consistent Hashing**: How files are distributed across nodes
- **Replication**: Ensuring data durability through multiple copies
- **Fault Tolerance**: System resilience when components fail
- **Load Balancing**: Even distribution of storage load
- **Microservices**: Service-oriented architecture patterns

## 🤝 Contributing

Contributions are welcome! Areas that could use improvement:

- Additional storage backends (S3, Azure Blob, etc.)
- Performance optimizations
- Enhanced error handling
- Additional API endpoints
- Testing improvements

## 📄 License

[Add your license information here]

---

**Happy coding! 🚀**

If you find this project useful, consider giving it a star ⭐ or contributing to its development.
