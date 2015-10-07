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

#include "buffer_t.h"

std::vector<cl::Platform> get_platforms()
{
    std::vector<cl::Platform> result;
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

std::vector<cl::Device> get_devices(const cl::Context &context)
{
    std::vector<cl::Device> result = context.getInfo<CL_CONTEXT_DEVICES>();
    if (result.empty())
    {
        throw std::runtime_error("No device was found");
    }
    return result;
}

cl::Program get_program(const cl::Context &context, const std::string &path)
{
    std::ifstream stream(path);
    std::string sources(std::istreambuf_iterator<char>(stream), (std::istreambuf_iterator<char>()));
    return cl::Program(context, cl::Program::Sources(1, std::make_pair(sources.c_str(), sources.length() + 1)));
}

#define WORK_GROUP_SIZE 256

void run(const cl::CommandQueue &queue, const cl::Kernel &kernel, size_t global,
         const cl::Buffer &buffer, size_t buffer_size, size_t offset)
{
    cl::KernelFunctor(kernel, queue, 0, global, WORK_GROUP_SIZE)(buffer, buffer_size, offset);
}

//#define USE_STD_IN
//#define USE_STD_OUT

//#define BUFFER_SIZE 1048576

int main()
{
    try
    {
        auto context = get_context(get_platforms()[0]);
        auto devices = get_devices(context);

        auto program = get_program(context, "prefix_sum.cl");
        program.build(devices);

        auto device = devices[0];
        cl::CommandQueue queue(context, device, 0);

        size_t size;
#ifdef BUFFER_SIZE
        size = BUFFER_SIZE;
        buffer_f buffer(size, 1);
#else
#ifdef USE_STD_IN
        std::cin >> size;
#else
        std::ifstream input_stream("input.txt");
        input_stream >> size;
#endif

        buffer_f buffer(size);
#ifdef USE_STD_IN
        std::cin >> buffer;
#else
        input_stream >> buffer;
#endif
#endif

        const size_t size_of_buffer = size_of(buffer);
        cl::Buffer cl_buffer(context, CL_MEM_READ_WRITE, capacity_of(buffer));
        queue.enqueueWriteBuffer(cl_buffer, CL_TRUE, 0, size_of_buffer, (float*) buffer);

        cl::Kernel reduce(program, "reduce");
        const size_t capacity = buffer.capacity();
        for (size_t offset = 1; capacity / offset >= 2 * WORK_GROUP_SIZE; offset <<= 1)
        {
            run(queue, reduce, capacity / offset, cl_buffer, capacity, offset);
        }
        if (capacity < 2 * WORK_GROUP_SIZE)
        {
            run(queue, reduce, WORK_GROUP_SIZE, cl_buffer, capacity, 1);
        }

        cl::Kernel sweep(program, "sweep");
        run(queue, sweep, WORK_GROUP_SIZE, cl_buffer, capacity, capacity / 2);
        for (size_t offset = capacity / WORK_GROUP_SIZE / 4; offset > 0; offset >>= 1)
        {
            run(queue, sweep, capacity / offset, cl_buffer, capacity, offset);
        }

        queue.enqueueReadBuffer(cl_buffer, CL_TRUE, 0, size_of_buffer, (float*) buffer);
        queue.finish();

#ifdef USE_STD_OUT
        std::cout << buffer;
#else
        std::ofstream output_stream("output.txt");
        output_stream << buffer;
#endif
    }
    catch (cl::Error &e)
    {
        std::cerr << e.what() << ": " << e.err() << std::endl;
        return 1;
    }
    catch (std::runtime_error &e)
    {
        std::cerr << e.what() << std::endl;
        return 1;
    }

    return 0;
}