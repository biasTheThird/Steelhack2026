#define UNICODE
#define _UNICODE

#include <windows.h>
#include <shellapi.h>

#include <filesystem>
#include <string>
#include <vector>

namespace fs = std::filesystem;

namespace {

fs::path executablePath() {
    std::vector<wchar_t> buffer(1024);
    for (;;) {
        const DWORD length = GetModuleFileNameW(nullptr, buffer.data(),
                                                static_cast<DWORD>(buffer.size()));
        if (length == 0) {
            return {};
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

fs::path findJava() {
    const DWORD required = GetEnvironmentVariableW(L"JAVA_HOME", nullptr, 0);
    if (required > 0) {
        std::vector<wchar_t> home(required);
        if (GetEnvironmentVariableW(L"JAVA_HOME", home.data(), required) > 0) {
            const fs::path javaw = fs::path(home.data()) / L"bin" / L"javaw.exe";
            if (fs::is_regular_file(javaw)) {
                return javaw;
            }
            const fs::path java = fs::path(home.data()) / L"bin" / L"java.exe";
            if (fs::is_regular_file(java)) {
                return java;
            }
        }
    }

    std::vector<wchar_t> result(32768);
    for (const wchar_t* name : {L"javaw.exe", L"java.exe"}) {
        const DWORD length = SearchPathW(nullptr, name, nullptr,
                                         static_cast<DWORD>(result.size()), result.data(),
                                         nullptr);
        if (length > 0 && length < result.size()) {
            return fs::path(std::wstring(result.data(), length));
        }
    }
    return {};
}

void showError(const std::wstring& message) {
    MessageBoxW(nullptr, message.c_str(), L"Guess-A-Morph", MB_OK | MB_ICONERROR);
}

}  // namespace

int WINAPI wWinMain(HINSTANCE, HINSTANCE, PWSTR, int) {
    const fs::path launcher = executablePath();
    if (launcher.empty()) {
        showError(L"Could not locate Guess-A-Morph.exe.");
        return 1;
    }
    const fs::path root = launcher.parent_path();
    const fs::path jar = root / L"MorphGuess.jar";
    if (!fs::is_regular_file(jar)) {
        showError(L"MorphGuess.jar is missing. Run Build-Guess-A-Morph.exe first.");
        return 1;
    }
    const fs::path java = findJava();
    if (java.empty()) {
        showError(L"Java was not found. Install Java or set JAVA_HOME, then try again.");
        return 1;
    }

    int argumentCount = 0;
    LPWSTR* arguments = CommandLineToArgvW(GetCommandLineW(), &argumentCount);
    std::wstring command = quoteArgument(java.wstring()) + L" -jar " +
                           quoteArgument(jar.wstring());
    if (arguments) {
        for (int index = 1; index < argumentCount; ++index) {
            command.push_back(L' ');
            command += quoteArgument(arguments[index]);
        }
        LocalFree(arguments);
    }

    std::vector<wchar_t> mutableCommand(command.begin(), command.end());
    mutableCommand.push_back(L'\0');
    STARTUPINFOW startup{};
    startup.cb = sizeof(startup);
    PROCESS_INFORMATION process{};
    if (!CreateProcessW(java.c_str(), mutableCommand.data(), nullptr, nullptr, FALSE, 0,
                        nullptr, root.c_str(), &startup, &process)) {
        showError(L"Java could not start MorphGuess.jar.");
        return 1;
    }

    CloseHandle(process.hThread);
    WaitForSingleObject(process.hProcess, INFINITE);
    DWORD exitCode = 1;
    GetExitCodeProcess(process.hProcess, &exitCode);
    CloseHandle(process.hProcess);
    return static_cast<int>(exitCode);
}
