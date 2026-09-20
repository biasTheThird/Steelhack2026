#define UNICODE
#define _UNICODE

#include <windows.h>

#include <algorithm>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>

namespace fs = std::filesystem;

namespace {

constexpr char kLauncherMarker[16] = {
    'G', 'A', 'M', 'L', 'A', 'U', 'N', 'C', 'H', 'E', 'R', '_', 'V', '1', '\r', '\n'};

std::wstring windowsError(DWORD code) {
    wchar_t* message = nullptr;
    const DWORD size = FormatMessageW(
        FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM |
            FORMAT_MESSAGE_IGNORE_INSERTS,
        nullptr, code, 0, reinterpret_cast<wchar_t*>(&message), 0, nullptr);
    std::wstring result = size && message ? std::wstring(message, size)
                                          : L"Windows error " + std::to_wstring(code);
    if (message) {
        LocalFree(message);
    }
    while (!result.empty() && (result.back() == L'\r' || result.back() == L'\n')) {
        result.pop_back();
    }
    return result;
}

fs::path executablePath() {
    std::vector<wchar_t> buffer(1024);
    for (;;) {
        const DWORD length = GetModuleFileNameW(nullptr, buffer.data(),
                                                static_cast<DWORD>(buffer.size()));
        if (length == 0) {
            throw std::runtime_error("Could not locate the build executable.");
        }
        if (length < buffer.size() - 1) {
            return fs::path(std::wstring(buffer.data(), length));
        }
        buffer.resize(buffer.size() * 2);
    }
}

std::wstring quoteArgument(const std::wstring& value) {
    if (value.empty()) {
        return L"\"\"";
    }
    if (value.find_first_of(L" \t\n\v\"") == std::wstring::npos) {
        return value;
    }

    std::wstring quoted = L"\"";
    std::size_t slashes = 0;
    for (const wchar_t c : value) {
        if (c == L'\\') {
            ++slashes;
        } else if (c == L'\"') {
            quoted.append(slashes * 2 + 1, L'\\');
            quoted.push_back(L'\"');
            slashes = 0;
        } else {
            quoted.append(slashes, L'\\');
            slashes = 0;
            quoted.push_back(c);
        }
    }
    quoted.append(slashes * 2, L'\\');
    quoted.push_back(L'\"');
    return quoted;
}

std::string utf8(const std::wstring& value) {
    if (value.empty()) {
        return {};
    }
    const int size = WideCharToMultiByte(CP_UTF8, 0, value.data(),
                                         static_cast<int>(value.size()), nullptr, 0,
                                         nullptr, nullptr);
    if (size <= 0) {
        throw std::runtime_error("Could not convert a path to UTF-8.");
    }
    std::string result(static_cast<std::size_t>(size), '\0');
    WideCharToMultiByte(CP_UTF8, 0, value.data(), static_cast<int>(value.size()),
                        result.data(), size, nullptr, nullptr);
    return result;
}

fs::path findJavac() {
    const DWORD required = GetEnvironmentVariableW(L"JAVA_HOME", nullptr, 0);
    if (required > 0) {
        std::vector<wchar_t> home(required);
        if (GetEnvironmentVariableW(L"JAVA_HOME", home.data(), required) > 0) {
            const fs::path candidate = fs::path(home.data()) / L"bin" / L"javac.exe";
            if (fs::is_regular_file(candidate)) {
                return candidate;
            }
        }
    }

    std::vector<wchar_t> result(32768);
    const DWORD length = SearchPathW(nullptr, L"javac.exe", nullptr,
                                     static_cast<DWORD>(result.size()), result.data(),
                                     nullptr);
    if (length > 0 && length < result.size()) {
        return fs::path(std::wstring(result.data(), length));
    }

    throw std::runtime_error(
        "javac.exe was not found. Install a JDK and set JAVA_HOME or add its bin "
        "folder to PATH.");
}

int runProcess(const fs::path& program, const std::vector<std::wstring>& arguments,
               const fs::path& workingDirectory) {
    std::wstring command = quoteArgument(program.wstring());
    for (const std::wstring& argument : arguments) {
        command.push_back(L' ');
        command += quoteArgument(argument);
    }
    std::vector<wchar_t> mutableCommand(command.begin(), command.end());
    mutableCommand.push_back(L'\0');

    STARTUPINFOW startup{};
    startup.cb = sizeof(startup);
    PROCESS_INFORMATION process{};
    if (!CreateProcessW(program.c_str(), mutableCommand.data(), nullptr, nullptr, TRUE,
                        0, nullptr, workingDirectory.c_str(), &startup, &process)) {
        const std::wstring message = windowsError(GetLastError());
        throw std::runtime_error("Could not start javac.exe: " + utf8(message));
    }

    WaitForSingleObject(process.hProcess, INFINITE);
    DWORD exitCode = 1;
    GetExitCodeProcess(process.hProcess, &exitCode);
    CloseHandle(process.hThread);
    CloseHandle(process.hProcess);
    return static_cast<int>(exitCode);
}

std::vector<fs::path> javaSources(const fs::path& root) {
    std::vector<fs::path> sources;
    const fs::path entry = root / L"MorphGuess.java";
    if (!fs::is_regular_file(entry)) {
        throw std::runtime_error(
            "MorphGuess.java is missing. Keep the build executable in the project root.");
    }
    sources.push_back(entry);

    for (const wchar_t* folderName : {L"game", L"morph", L"net", L"ui"}) {
        const fs::path folder = root / folderName;
        if (!fs::is_directory(folder)) {
            throw std::runtime_error("A required Java source folder is missing.");
        }
        for (const fs::directory_entry& item : fs::recursive_directory_iterator(folder)) {
            if (item.is_regular_file() && item.path().extension() == L".java") {
                sources.push_back(item.path());
            }
        }
    }

    std::sort(sources.begin(), sources.end());
    return sources;
}

void writeJavacArguments(const fs::path& file, const fs::path& classes,
                         const std::vector<fs::path>& sources) {
    std::ofstream output(file, std::ios::binary | std::ios::trunc);
    if (!output) {
        throw std::runtime_error("Could not create the javac argument file.");
    }
    output << "-encoding\nUTF-8\n-d\n\"" << utf8(classes.generic_wstring())
           << "\"\n";
    for (const fs::path& source : sources) {
        output << '"' << utf8(source.generic_wstring()) << "\"\n";
    }
}

std::uint32_t crc32(const std::vector<unsigned char>& data) {
    std::uint32_t value = 0xffffffffu;
    for (const unsigned char byte : data) {
        value ^= byte;
        for (int bit = 0; bit < 8; ++bit) {
            value = (value >> 1) ^ (0xedb88320u & (0u - (value & 1u)));
        }
    }
    return value ^ 0xffffffffu;
}

void put16(std::ostream& output, std::uint16_t value) {
    output.put(static_cast<char>(value & 0xff));
    output.put(static_cast<char>((value >> 8) & 0xff));
}

void put32(std::ostream& output, std::uint32_t value) {
    put16(output, static_cast<std::uint16_t>(value & 0xffff));
    put16(output, static_cast<std::uint16_t>((value >> 16) & 0xffff));
}

std::vector<unsigned char> readFile(const fs::path& file) {
    std::ifstream input(file, std::ios::binary);
    if (!input) {
        throw std::runtime_error("Could not read a compiled class file.");
    }
    input.seekg(0, std::ios::end);
    const std::streamoff size = input.tellg();
    if (size < 0 || static_cast<std::uint64_t>(size) > 0xffffffffu) {
        throw std::runtime_error("A compiled class file is too large for the JAR.");
    }
    input.seekg(0, std::ios::beg);
    std::vector<unsigned char> data(static_cast<std::size_t>(size));
    if (!data.empty()) {
        input.read(reinterpret_cast<char*>(data.data()), size);
    }
    return data;
}

struct JarEntry {
    std::string name;
    std::vector<unsigned char> data;
    std::uint32_t checksum = 0;
    std::uint32_t localOffset = 0;
};

void createJar(const fs::path& classes, const fs::path& outputFile) {
    std::vector<JarEntry> entries;
    const std::string manifest =
        "Manifest-Version: 1.0\r\nMain-Class: MorphGuess\r\n\r\n";
    entries.push_back({"META-INF/MANIFEST.MF",
                       std::vector<unsigned char>(manifest.begin(), manifest.end())});

    for (const fs::directory_entry& item : fs::recursive_directory_iterator(classes)) {
        if (!item.is_regular_file() || item.path().extension() != L".class") {
            continue;
        }
        JarEntry entry;
        entry.name = fs::relative(item.path(), classes).generic_u8string();
        entry.data = readFile(item.path());
        entries.push_back(std::move(entry));
    }
    if (entries.size() == 1) {
        throw std::runtime_error("javac did not produce any class files.");
    }
    std::sort(entries.begin() + 1, entries.end(),
              [](const JarEntry& left, const JarEntry& right) {
                  return left.name < right.name;
              });

    SYSTEMTIME time{};
    GetLocalTime(&time);
    const std::uint16_t dosTime = static_cast<std::uint16_t>(
        (time.wHour << 11) | (time.wMinute << 5) | (time.wSecond / 2));
    const std::uint16_t dosDate = static_cast<std::uint16_t>(
        ((std::max<int>(1980, time.wYear) - 1980) << 9) | (time.wMonth << 5) |
        time.wDay);

    std::ofstream output(outputFile, std::ios::binary | std::ios::trunc);
    if (!output) {
        throw std::runtime_error("Could not create MorphGuess.jar.");
    }

    for (JarEntry& entry : entries) {
        entry.checksum = crc32(entry.data);
        const std::streamoff offset = output.tellp();
        if (offset < 0 || static_cast<std::uint64_t>(offset) > 0xffffffffu ||
            entry.name.size() > 0xffffu || entry.data.size() > 0xffffffffu) {
            throw std::runtime_error("The JAR is too large for this build tool.");
        }
        entry.localOffset = static_cast<std::uint32_t>(offset);
        put32(output, 0x04034b50);
        put16(output, 20);
        put16(output, 0);
        put16(output, 0);  // Stored: Java class files are already compact.
        put16(output, dosTime);
        put16(output, dosDate);
        put32(output, entry.checksum);
        put32(output, static_cast<std::uint32_t>(entry.data.size()));
        put32(output, static_cast<std::uint32_t>(entry.data.size()));
        put16(output, static_cast<std::uint16_t>(entry.name.size()));
        put16(output, 0);
        output.write(entry.name.data(), static_cast<std::streamsize>(entry.name.size()));
        if (!entry.data.empty()) {
            output.write(reinterpret_cast<const char*>(entry.data.data()),
                         static_cast<std::streamsize>(entry.data.size()));
        }
    }

    const std::streamoff centralOffsetValue = output.tellp();
    if (centralOffsetValue < 0 ||
        static_cast<std::uint64_t>(centralOffsetValue) > 0xffffffffu ||
        entries.size() > 0xffffu) {
        throw std::runtime_error("The JAR directory is too large.");
    }
    const std::uint32_t centralOffset = static_cast<std::uint32_t>(centralOffsetValue);

    for (const JarEntry& entry : entries) {
        put32(output, 0x02014b50);
        put16(output, 20);
        put16(output, 20);
        put16(output, 0);
        put16(output, 0);
        put16(output, dosTime);
        put16(output, dosDate);
        put32(output, entry.checksum);
        put32(output, static_cast<std::uint32_t>(entry.data.size()));
        put32(output, static_cast<std::uint32_t>(entry.data.size()));
        put16(output, static_cast<std::uint16_t>(entry.name.size()));
        put16(output, 0);
        put16(output, 0);
        put16(output, 0);
        put16(output, 0);
        put32(output, 0);
        put32(output, entry.localOffset);
        output.write(entry.name.data(), static_cast<std::streamsize>(entry.name.size()));
    }

    const std::streamoff centralEndValue = output.tellp();
    if (centralEndValue < centralOffsetValue ||
        static_cast<std::uint64_t>(centralEndValue - centralOffsetValue) >
            0xffffffffu) {
        throw std::runtime_error("The JAR directory is too large.");
    }
    const std::uint32_t centralSize =
        static_cast<std::uint32_t>(centralEndValue - centralOffsetValue);

    put32(output, 0x06054b50);
    put16(output, 0);
    put16(output, 0);
    put16(output, static_cast<std::uint16_t>(entries.size()));
    put16(output, static_cast<std::uint16_t>(entries.size()));
    put32(output, centralSize);
    put32(output, centralOffset);
    put16(output, 0);
    output.close();
    if (!output) {
        throw std::runtime_error("Writing MorphGuess.jar failed.");
    }
}

std::uint64_t readLittleEndian64(const unsigned char* bytes) {
    std::uint64_t value = 0;
    for (int index = 7; index >= 0; --index) {
        value = (value << 8) | bytes[index];
    }
    return value;
}

void extractLauncher(const fs::path& builder, const fs::path& outputFile) {
    std::ifstream input(builder, std::ios::binary);
    if (!input) {
        throw std::runtime_error("Could not reopen the build executable.");
    }
    input.seekg(0, std::ios::end);
    const std::streamoff totalSize = input.tellg();
    constexpr std::streamoff footerSize = sizeof(kLauncherMarker) + sizeof(std::uint64_t);
    if (totalSize < footerSize) {
        throw std::runtime_error("The embedded launcher is missing.");
    }

    unsigned char footer[footerSize]{};
    input.seekg(totalSize - footerSize, std::ios::beg);
    input.read(reinterpret_cast<char*>(footer), footerSize);
    if (!input || !std::equal(std::begin(kLauncherMarker), std::end(kLauncherMarker),
                              reinterpret_cast<const char*>(footer))) {
        throw std::runtime_error(
            "The embedded launcher is missing. Rebuild with windows/build-windows.ps1.");
    }
    const std::uint64_t launcherSize = readLittleEndian64(footer + sizeof(kLauncherMarker));
    if (launcherSize == 0 || launcherSize > static_cast<std::uint64_t>(totalSize - footerSize)) {
        throw std::runtime_error("The embedded launcher is corrupt.");
    }

    input.seekg(totalSize - footerSize - static_cast<std::streamoff>(launcherSize),
                std::ios::beg);
    std::ofstream output(outputFile, std::ios::binary | std::ios::trunc);
    if (!output) {
        throw std::runtime_error("Could not create the launcher executable.");
    }
    std::vector<char> buffer(64 * 1024);
    std::uint64_t remaining = launcherSize;
    while (remaining > 0) {
        const std::streamsize amount = static_cast<std::streamsize>(
            std::min<std::uint64_t>(remaining, buffer.size()));
        input.read(buffer.data(), amount);
        if (input.gcount() != amount) {
            throw std::runtime_error("The embedded launcher could not be read.");
        }
        output.write(buffer.data(), amount);
        remaining -= static_cast<std::uint64_t>(amount);
    }
    output.close();
    if (!output) {
        throw std::runtime_error("Writing the launcher executable failed.");
    }
}

bool tryReplaceFile(const fs::path& temporary, const fs::path& destination) {
    if (!MoveFileExW(temporary.c_str(), destination.c_str(), MOVEFILE_REPLACE_EXISTING |
                                                           MOVEFILE_WRITE_THROUGH)) {
        const DWORD code = GetLastError();
        if (code == ERROR_ACCESS_DENIED || code == ERROR_SHARING_VIOLATION) {
            return false;
        }
        const std::wstring message = windowsError(code);
        throw std::runtime_error("Could not replace " + utf8(destination.filename().wstring()) +
                                 ": " + utf8(message));
    }
    return true;
}

void replaceFile(const fs::path& temporary, const fs::path& destination) {
    if (!tryReplaceFile(temporary, destination)) {
        throw std::runtime_error("Could not replace " +
                                 utf8(destination.filename().wstring()) +
                                 " because it is currently running or open.");
    }
}

}  // namespace

int wmain() {
    try {
        const fs::path builder = executablePath();
        const fs::path root = builder.parent_path();
        const fs::path buildDirectory = root / L"build" / L"windows";
        const fs::path classes = buildDirectory / L"classes";
        const fs::path argumentsFile = buildDirectory / L"javac.args";
        const fs::path jarTemporary = root / L"MorphGuess.jar.tmp";
        const fs::path launcherTemporary = root / L"Guess-A-Morph.exe.tmp";

        std::wcout << L"Building Guess-A-Morph in " << root.wstring() << L"\n\n";
        const fs::path javac = findJavac();
        std::wcout << L"JDK compiler: " << javac.wstring() << L"\n";

        std::error_code error;
        fs::remove_all(classes, error);
        if (error) {
            throw std::runtime_error("Could not clear build/windows/classes.");
        }
        fs::create_directories(classes);
        const std::vector<fs::path> sources = javaSources(root);
        writeJavacArguments(argumentsFile, classes, sources);

        std::wcout << L"Compiling " << sources.size() << L" Java source files...\n";
        const int compileResult =
            runProcess(javac, {L"@" + argumentsFile.wstring()}, root);
        if (compileResult != 0) {
            throw std::runtime_error("javac failed. The existing game files were not replaced.");
        }

        fs::remove(jarTemporary, error);
        fs::remove(launcherTemporary, error);
        std::wcout << L"Packaging MorphGuess.jar...\n";
        createJar(classes, jarTemporary);
        std::wcout << L"Creating Guess-A-Morph.exe...\n";
        extractLauncher(builder, launcherTemporary);

        replaceFile(jarTemporary, root / L"MorphGuess.jar");

        fs::path launcherOutput = root / L"Guess-A-Morph.exe";
        if (!tryReplaceFile(launcherTemporary, launcherOutput)) {
            launcherOutput = root / L"Guess-A-Morph-built.exe";
            replaceFile(launcherTemporary, launcherOutput);
            std::wcout
                << L"The existing Guess-A-Morph.exe is running, so the new launcher "
                   L"was saved under a different name.\n";
        }

        std::wcout << L"\nBuild complete. Run " << launcherOutput.filename().wstring()
                   << L" to start the game.\n";
        return 0;
    } catch (const std::exception& failure) {
        std::cerr << "\nBUILD FAILED: " << failure.what() << "\n";
        return 1;
    }
}
