#define __CL_ENABLE_EXCEPTIONS

#if defined(__APPLE__) || defined(__MACOSX)
#include <OpenCL/cl.h>
#else
#include <CL/cl.h>
#endif

#if defined(__APPLE__) || defined(__MACOSX)
#include "cl.hpp"
#else
#include <CL/cl.hpp>
#endif

#include <vector>
#include <iostream>
#include <fstream>
#include <iomanip>

#include "matrix_t.h"

using std::string;
using std::vector;
using std::runtime_error;

vector<cl::Platform> get_platforms()
{
    vector<cl::Platform> result;
    cl::Platform::get(&result);
    if (result.empty())
    {
        throw std::runtime_error("No platform was found");
    }
    return result;
}

cl::Context get_context(const cl::Platform &platform) {
    cl_context_properties properties[] = { CL_CONTEXT_PLATFORM, (cl_context_properties)(platform)(), 0 };
    return cl::Context(CL_DEVICE_TYPE_GPU, properties);
}

vector<cl::Device> get_devices(const cl::Context &context)
{
    std::vector<cl::Device> result = context.getInfo<CL_CONTEXT_DEVICES>();
    if (result.empty())
    {
        throw std::runtime_error("No device was found");
    }
    return result;
}

cl::Program get_program(const cl::Context &context, const string &path)
{
    std::ifstream stream(path);
    std::string sources(std::istreambuf_iterator<char>(stream), (std::istreambuf_iterator<char>()));
    return cl::Program(context, cl::Program::Sources(1, std::make_pair(sources.c_str(), sources.length() + 1)));
}

cl::Buffer enqueue_buffer(const cl::Context &context, const cl::CommandQueue &queue, const matrix_f &matrix)
{
    auto buffer_size = matrix.buffer_size();
    cl::Buffer buffer(context, CL_MEM_READ_ONLY, buffer_size);
    queue.enqueueWriteBuffer(buffer, CL_TRUE, 0, buffer_size, (const float*) matrix);
    return buffer;
}

cl::KernelFunctor get_functor(const cl::Device &device,
                              const cl::CommandQueue &queue,
                              const cl::Program &program,
                              const string &program_name,
                              size_t size)
{
    cl::Kernel kernel(program, program_name.c_str());
    auto work_group_info = kernel.getWorkGroupInfo<CL_KERNEL_WORK_GROUP_SIZE>(device);
    auto global_work_size = (size * size / work_group_info + 1) * work_group_info;
    return cl::KernelFunctor(kernel, queue, cl::NullRange, cl::NDRange(global_work_size), cl::NDRange(work_group_info));
}

//#define USE_STD_IN
//#define USE_STD_OUT

//#define FST_SIZE 31
//#define SND_SIZE 9

int main()
{
    try
    {
        auto context = get_context(get_platforms()[0]);
        auto devices = get_devices(context);

        auto program = get_program(context, "convolution.cl");
        program.build(devices);

        auto device = devices[0];
        cl::CommandQueue queue(context, device, 0);

        size_t fst_size, snd_size;
#if defined(FST_SIZE) && defined(SND_SIZE)
        fst_size = FST_SIZE;
        snd_size = SND_SIZE;
        matrix_f fst(FST_SIZE, 1), snd(SND_SIZE, 1);
#else
#ifdef USE_STD_IN
        std::cin >> fst_size >> snd_size;
#else
        std::ifstream input_stream("input.txt");
        input_stream >> fst_size >> snd_size;
#endif

        matrix_f fst(fst_size), snd(snd_size);
#ifdef USE_STD_IN
        std::cin >> fst >> snd;
#else
        input_stream >> fst >> snd;
#endif
#endif

        auto fst_buffer = enqueue_buffer(context, queue, fst);
        auto snd_buffer = enqueue_buffer(context, queue, snd);
        cl::Buffer result_buffer(context, CL_MEM_WRITE_ONLY, fst.buffer_size());
        queue.finish();

        auto convolution = get_functor(device, queue, program, "convolution", fst_size);
        convolution(fst_buffer, fst_size, snd_buffer, snd_size, result_buffer).wait();

        matrix_f result(fst_size);
        queue.enqueueReadBuffer(result_buffer, CL_TRUE, 0, fst.buffer_size(), (float*) result);

#ifdef USE_STD_OUT
        std::cout << result;
#else
        std::ofstream output_stream("output.txt");
        output_stream << result;
#endif
    }
    catch (cl::Error &e)
    {
        std::cerr << e.what() << ": " << e.err() << std::endl;
        return 1;
    }
    catch (runtime_error &e)
    {
        std::cerr << e.what() << std::endl;
        return 1;
    }

    return 0;
}